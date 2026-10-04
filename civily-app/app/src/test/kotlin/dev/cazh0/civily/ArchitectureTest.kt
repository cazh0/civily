package dev.cazh0.civily

import dev.cazh0.civily.core.result.Outcome
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.zip.ZipFile
import kotlin.coroutines.Continuation

/**
 * ARCHITECTURE.md §1, A1–A7, each as a check that fails `testDebugUnitTest`.
 *
 * A3 reads the compiled classes: a way out can hide behind an overload the source never spells
 * (Coil's singleton `AsyncImage`). The rest read the source with comments stripped and, where a
 * rule is not about text, string contents blanked. A red run lists every offender.
 */
class ArchitectureTest {

    @Test
    fun `A1 imports point down, data imports no Compose, only feature and nav reach the root`() {
        val offenders = sources.flatMap { source ->
            val rank = TIERS.indexOf(source.tier)
            val upward = source.references.filter { ref ->
                val target = tierOf(ref)
                if (target == ROOT) {
                    source.tier !in ROOT_READERS && ref.removePrefix("$BASE.").substringBefore('.') !in ANY_TIER_MAY_READ
                } else {
                    TIERS.indexOf(target) > rank
                }
            }
            val compose = listOfNotNull("androidx.compose".takeIf { source.tier == "data" && it in source.code })
            (upward + compose).map { "${source.path} → $it" }
        }
        assertClean("A1", offenders)
    }

    @Test
    fun `A2 a repository returns Outcome and never throws, and every CivilyError carries its string`() {
        val repositories = sources.filter { it.tier == "data" && it.file.name.endsWith("Repository.kt") }
        assertTrue("No repositories found", repositories.isNotEmpty())

        val leaks = repositories.flatMap { repository ->
            val type = Class.forName(
                "${repository.pkg}.${repository.file.nameWithoutExtension}",
                false,
                javaClass.classLoader,
            )
            val signatures = type.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic && !it.returnsOutcome() }
                .map { "${repository.path}: ${it.name} does not return Outcome" }
            signatures + THROWS.findAll(repository.code).map { "${repository.path}: ${it.value.trim()}" }
        }

        val strings = File(RES, "values").listFiles { file -> file.extension == "xml" }.orEmpty()
            .flatMap { STRING_RESOURCE.findAll(it.readText()).map { match -> match.groupValues[1] } }
            .toSet()
        val errors = sources.flatMap { source ->
            ERROR_SUBTYPE.findAll(source.code).map { source to it.groupValues[1].trim() }
        }
        assertTrue("No CivilyError subtypes found", errors.isNotEmpty())
        val unworded = errors
            .filterNot { (_, argument) -> STRING_REF.matchEntire(argument)?.groupValues?.get(1) in strings }
            .map { (source, argument) -> "${source.path}: CivilyError($argument)" }

        assertClean("A2", leaks + unworded)
    }

    @Test
    fun `A3 one way out - API through NsClient, images through AppGraph, one OkHttpClient`() {
        val classes = compiledClasses()
        assertTrue("No compiled classes found", classes.isNotEmpty())

        val offenders = classes.flatMap { (name, refs) ->
            refs.mapNotNull { (owner, member) ->
                val builds = owner in CLIENT_FACTORIES && (member == "<init>" || member in FACTORY_CALLS)
                when {
                    owner in BANNED_OWNERS || BANNED_PREFIXES.any(owner::startsWith) -> "$name uses $owner"
                    builds && !name.startsWith(GRAPH) -> "$name builds $owner"
                    member in CALLS && !name.startsWith(CLIENT) -> "$name calls $owner.$member"
                    else -> null
                }
            }
        }.distinct()

        val graph = sources.single { it.path == "AppGraph.kt" }
        val clients = OKHTTP_BUILDER.findAll(graph.code).count()
        val extra = listOfNotNull("AppGraph.kt builds $clients OkHttpClients".takeIf { clients != 1 })

        assertClean("A3", offenders + extra)
    }

    @Test
    fun `A4 on-screen colours, sizes and durations live in ui-theme`() {
        val offenders = sources.filterNot { it.path.startsWith("ui/theme/") }.flatMap { source ->
            // Why the newspaper keeps its sizes: DESIGN_RULES.md §1.1, a paper's frame measurements
            // stay beside its drawing. Its colours and durations are still the theme's.
            val patterns = if (source.path.startsWith(NEWSPAPER)) COLOURS + DURATIONS else COLOURS + SIZES + DURATIONS
            patterns.flatMap { it.findAll(source.code) }
                .filter { match -> match.groupValues.getOrNull(1)?.replace("_", "")?.toDouble() != 0.0 }
                .map { "${source.path}: ${it.value}" }
        }
        assertClean("A4", offenders)
    }

    @Test
    fun `A5 every user-visible string is a resource`() {
        val offenders = sources.filter { it.tier in UI_TIERS }.flatMap { source ->
            VISIBLE_TEXT.findAll(source.withStrings)
                .filter { LETTER.containsMatchIn(it.groupValues[1].replace(NOT_WORDS, "")) }
                .map { "${source.path}: ${it.value}" }
        }
        assertClean("A5", offenders)
    }

    @Test
    fun `A6 routes are written only in Routes, navigation goes only through NavActions`() {
        val host = sources.single { it.tier == "nav" && "object Routes" in it.code }
        val routes = host.block("object Routes")
        val actions = host.block("class NavActions")

        val written = STRING.findAll(host.withStrings).filter { it.range.first in routes }.map { it.groupValues[1] }
        val heads = written.filter { '/' in it }.map { it.substringBefore('/') + "/" }.toSet()
        val exact = written.filterNot { '/' in it }.toSet()
        assertTrue("Routes holds no route strings", heads.isNotEmpty() && exact.isNotEmpty())

        val offenders = sources.flatMap { source ->
            val inRoutes = { at: Int -> source === host && at in routes }
            val inActions = { at: Int -> source === host && at in actions }
            val stray = STRING.findAll(source.withStrings)
                .filter { source.tier in UI_TIERS + "nav" && !inRoutes(it.range.first) }
                .filter { match -> match.groupValues[1].let { it in exact || heads.any(it::startsWith) } }
                .map { "${source.path}: route ${it.value}" }
            val navigation = NAVIGATE.findAll(source.code)
                .filterNot { inActions(it.range.first) }
                .map { "${source.path}: ${it.value}" }
            val controller = listOfNotNull(
                "${source.path}: androidx.navigation"
                    .takeIf { source.tier != "nav" && "androidx.navigation" in source.code },
            )
            stray + navigation + controller
        }
        assertClean("A6", offenders)
    }

    @Test
    fun `A7 a file in core-text or ui-component is reached by two features or by data`() {
        val shared = sources.filter { it.isShared }
        assertTrue("No shared files found", shared.isNotEmpty())

        val offenders = shared.filterNot { it.path in NAMED_BY_RULES }.mapNotNull { target ->
            // Why only shared files are walked through: reach counts calls through core/text and
            // ui/component, never through another feature's section (DECISIONS.md, Architecture).
            val seen = mutableSetOf(target)
            val queue = ArrayDeque(listOf(target))
            val features = sortedSetOf<String>()
            var data = false
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                sources.filter { it !== current && it.uses(current) }.forEach { user ->
                    when {
                        user.isShared -> if (seen.add(user)) queue += user
                        user.tier == "feature" -> features += user.feature
                        user.tier == "data" -> data = true
                    }
                }
            }
            "${target.path} reached by ${features.ifEmpty { setOf("nothing") }}"
                .takeUnless { data || features.size >= 2 }
        }
        assertClean("A7", offenders)
    }

    private class Source(val file: File) {
        val path = file.relativeTo(SRC).invariantSeparatorsPath
        private val text = file.readText()

        /** Comments blanked, strings as written. */
        val withStrings = lex(text, keepStrings = true)

        /** Comments and string contents blanked. Same offsets as [withStrings]. */
        val code = lex(text, keepStrings = false)

        val pkg = requireNotNull(PACKAGE.find(code)) { "$path has no package" }.groupValues[1]
        val tier = pkg.removePrefix(BASE).removePrefix(".").substringBefore('.')
        val feature = pkg.removePrefix("$BASE.feature.").substringBefore('.')
        val isShared = path.startsWith("core/text/") || path.startsWith("ui/component/")

        /** Every name this file spells out under [BASE]: its imports and any qualified use. */
        val references = FQ_NAME.findAll(code.replaceFirst(PACKAGE, "")).map { it.value }.toList()

        private val identifiers = IDENTIFIER.findAll(code).map { it.value }.toSet()

        /** Top-level names another file can reach. */
        private val declarations = DECLARATION.findAll(code)
            .filterNot { "private" in it.groupValues[1] }
            .map { it.groupValues[2] }
            .toSet()

        fun uses(other: Source): Boolean = other.declarations.any { name ->
            val qualified = "${other.pkg}.$name"
            references.any { it == qualified || it.startsWith("$qualified.") } ||
                (pkg == other.pkg && name in identifiers)
        }

        /** The braces of the first declaration [header] opens. */
        fun block(header: String): IntRange {
            val open = code.indexOf('{', code.indexOf(header).also { require(it >= 0) { "$path has no $header" } })
            var depth = 0
            for (at in open until code.length) {
                when (code[at]) {
                    '{' -> depth++
                    '}' -> if (--depth == 0) return open..at
                }
            }
            error("$path: $header never closes")
        }
    }

    private companion object {
        const val BASE = "dev.cazh0.civily"
        val SRC = File("src/main/kotlin/dev/cazh0/civily")
        val RES = File("src/main/res")

        /** Lowest first. The package root, "", sits above every tier. */
        val TIERS = listOf("core", "data", "ui", "feature", "nav", "")
        const val ROOT = ""
        val ROOT_READERS = setOf("feature", "nav", ROOT)
        val ANY_TIER_MAY_READ = setOf("R", "BuildConfig")
        val UI_TIERS = setOf("ui", "feature", ROOT)

        const val NEWSPAPER = "feature/issues/newspaper/"
        val NAMED_BY_RULES = setOf("core/text/Population.kt") // RULES.md §3

        const val GRAPH = "dev/cazh0/civily/AppGraph"
        const val CLIENT = "dev/cazh0/civily/core/net/NsClient"
        val CLIENT_FACTORIES = setOf(
            "okhttp3/OkHttpClient",
            "okhttp3/OkHttpClient\$Builder",
            "coil/ImageLoader",
            "coil/ImageLoader\$Builder",
            "coil/ImageLoader\$Companion",
        )
        val FACTORY_CALLS = setOf("newBuilder", "create", "invoke")
        val CALLS = setOf("newCall", "newWebSocket")
        val BANNED_OWNERS = setOf(
            "coil/Coil",
            "java/net/URL",
            "java/net/URLConnection",
            "java/net/HttpURLConnection",
            "java/net/Socket",
            "javax/net/ssl/HttpsURLConnection",
        )
        val BANNED_PREFIXES = listOf("coil/compose/Singleton", "android/webkit/")

        val PACKAGE = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
        val FQ_NAME = Regex("""\bdev\.cazh0\.civily(?:\.\w+)+""")
        val IDENTIFIER = Regex("""\w+""")
        val DECLARATION = Regex(
            """^(?:@[\w.]+(?:\([^)\n]*\))?\s+)*((?:[a-z]+\s+)*?)""" +
                """(?:fun|val|var|class|object|interface|typealias)\s+(?:<[^>\n]*>\s*)?(?:[\w.<>?, *]+\.)?(\w+)""",
            RegexOption.MULTILINE,
        )
        val STRING = Regex(""""((?:[^"\\\n]|\\.)*)"""")

        val THROWS = Regex("""\bthrow\b|!!|\b(?:error|require|requireNotNull|check|checkNotNull)\(|Outcome<[^=\n{]*>\?""")
        val ERROR_SUBTYPE = Regex(""":\s*CivilyError\(([^)]*)\)""")
        val STRING_REF = Regex("""R\.string\.(\w+)""")
        val STRING_RESOURCE = Regex("""<string\s+name="(\w+)"""")

        val OKHTTP_BUILDER = Regex("""\bOkHttpClient\.Builder\(""")

        private const val NUMBER = """(\d[\d_]*(?:\.\d+)?)"""
        val COLOURS = listOf(
            Regex("""\bColor\(\s*(?:0x|\d)"""),
            Regex("""\bColor\.(?:White|Black|Red|Green|Blue|Gray|LightGray|DarkGray|Yellow|Cyan|Magenta)\b"""),
            Regex("""\bColor\.hs[lv]\("""),
            Regex("""\bandroid\.graphics\.Color\b"""),
        )
        val SIZES = listOf(
            Regex("""\b${NUMBER}f?\.(?:dp|sp|em)\b"""),
            Regex("""\b${NUMBER}f?\.(?:toDp|toSp)\("""),
            Regex("""\b(?:Dp|TextUnit)\(\s*$NUMBER"""),
        )
        val DURATIONS = listOf(
            Regex("""\b(?:tween|snap)(?:<[^>]*>)?\(\s*$NUMBER"""),
            Regex("""\b(?:durationMillis|delayMillis)\s*=\s*$NUMBER"""),
        )

        val VISIBLE_TEXT = Regex(
            """(?:\bText\(\s*|\b(?:text|contentDescription|stateDescription|onClickLabel)\s*=\s*|""" +
                """\bshowSnackbar\(\s*)"((?:[^"\\\n]|\\.)*)"""",
        )
        val NOT_WORDS = Regex("""\$\{[^}]*}|\$\w+|\\.""")
        val LETTER = Regex("""\p{L}""")

        val NAVIGATE = Regex("""\.(?:navigate|navigateUp|popBackStack)\(""")

        val sources: List<Source> by lazy {
            require(SRC.isDirectory) { "Run from the :app module; no ${SRC.absolutePath}" }
            SRC.walk().filter { it.extension == "kt" }.map(::Source).toList()
        }

        fun tierOf(reference: String): String =
            reference.removePrefix("$BASE.").substringBefore('.').takeIf { it in TIERS } ?: ROOT

        fun assertClean(rule: String, offenders: List<String>) =
            assertTrue("$rule broken:\n" + offenders.joinToString("\n"), offenders.isEmpty())

        fun Method.returnsOutcome(): Boolean {
            val continuation = genericParameterTypes.lastOrNull() as? ParameterizedType
            val result: Type = if (continuation?.rawType == Continuation::class.java) {
                (continuation.actualTypeArguments.single() as WildcardType).lowerBounds.single()
            } else {
                genericReturnType
            }
            return ((result as? ParameterizedType)?.rawType ?: result) == Outcome::class.java
        }

        /** Every compiled class of the app, with the (owner, member) pairs its constant pool names. */
        fun compiledClasses(): List<Pair<String, List<Pair<String, String?>>>> {
            val location = File(Outcome::class.java.protectionDomain.codeSource.location.toURI())
            val classes = if (location.isDirectory) {
                location.walk().filter { it.extension == "class" }
                    .map { it.relativeTo(location).invariantSeparatorsPath to it.readBytes() }.toList()
            } else {
                ZipFile(location).use { zip ->
                    zip.entries().toList().filter { it.name.endsWith(".class") }
                        .map { it.name to zip.getInputStream(it).readBytes() }
                }
            }
            return classes
                .filter { (name, _) -> name.startsWith("dev/cazh0/civily/") }
                .map { (name, bytes) -> name.removeSuffix(".class") to constantPoolRefs(bytes) }
        }

        /** The JVM class-file constant pool, read only as far as names: classes and member refs. */
        fun constantPoolRefs(bytes: ByteArray): List<Pair<String, String?>> = DataInputStream(bytes.inputStream()).run {
            skipBytes(8)
            val count = readUnsignedShort()
            val utf = arrayOfNulls<String>(count)
            val first = IntArray(count)
            val second = IntArray(count)
            val tags = IntArray(count)
            var index = 1
            while (index < count) {
                val tag = readUnsignedByte()
                tags[index] = tag
                when (tag) {
                    1 -> utf[index] = readUTF()
                    7, 8, 16, 19, 20 -> first[index] = readUnsignedShort()
                    9, 10, 11, 12, 17, 18 -> {
                        first[index] = readUnsignedShort()
                        second[index] = readUnsignedShort()
                    }
                    3, 4 -> readInt()
                    5, 6 -> {
                        readLong()
                        index++
                    }
                    15 -> {
                        readUnsignedByte()
                        readUnsignedShort()
                    }
                    else -> error("Unknown constant pool tag $tag")
                }
                index++
            }
            fun className(at: Int) = utf[first[at]]!!
            (1 until count).mapNotNull { at ->
                when (tags[at]) {
                    7 -> className(at) to null
                    9, 10, 11 -> className(first[at]) to utf[first[second[at]]]
                    else -> null
                }
            }
        }

        /** Comments become spaces; string contents too, unless [keepStrings]. Length and line breaks hold. */
        fun lex(text: String, keepStrings: Boolean): String {
            val out = StringBuilder(text.length)
            var at = 0
            fun blank(until: Int) {
                for (k in at until until) out.append(if (text[k] == '\n') '\n' else ' ')
                at = until
            }
            fun keep(until: Int) {
                out.append(text, at, until)
                at = until
            }
            while (at < text.length) {
                when {
                    text.startsWith("//", at) -> blank(text.indexOf('\n', at).takeIf { it >= 0 } ?: text.length)
                    text.startsWith("/*", at) -> blank(text.indexOf("*/", at + 2).takeIf { it >= 0 }?.plus(2) ?: text.length)
                    text[at] == '"' -> {
                        val end = stringEnd(text, at)
                        if (keepStrings) {
                            keep(end)
                        } else {
                            keep(at + 1)
                            blank(end - 1)
                            keep(end)
                        }
                    }
                    text[at] == '\'' -> keep(text.indexOf('\'', at + if (text[at + 1] == '\\') 3 else 2) + 1)
                    else -> keep(at + 1)
                }
            }
            return out.toString()
        }

        /** Just past the string literal opening at [start]. A template may hold strings of its own. */
        fun stringEnd(text: String, start: Int): Int {
            val raw = text.startsWith("\"\"\"", start)
            var at = start + if (raw) 3 else 1
            while (at < text.length) {
                when {
                    raw && text.startsWith("\"\"\"", at) -> {
                        var end = at + 3
                        while (end < text.length && text[end] == '"') end++
                        return end
                    }
                    !raw && text[at] == '"' -> return at + 1
                    !raw && text[at] == '\\' -> at += 2
                    text.startsWith("\${", at) -> at = templateEnd(text, at + 2)
                    else -> at++
                }
            }
            return text.length
        }

        fun templateEnd(text: String, start: Int): Int {
            var depth = 1
            var at = start
            while (at < text.length && depth > 0) {
                when (text[at]) {
                    '"' -> {
                        at = stringEnd(text, at)
                        continue
                    }
                    '{' -> depth++
                    '}' -> depth--
                }
                at++
            }
            return at
        }
    }
}
