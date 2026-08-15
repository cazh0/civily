package dev.cazh0.civily.data.issues

import dev.cazh0.civily.core.net.NsUrl
import dev.cazh0.civily.core.result.CivilyError
import dev.cazh0.civily.core.result.Outcome
import dev.cazh0.civily.core.text.HtmlEntities
import dev.cazh0.civily.core.text.NsId
import java.util.Locale

/**
 * Reads the NationStates issue pages the same way the browser does.
 *
 * Why this exists beside the XML DTOs: the API result is accurate for command responses, but
 * the exact aftermath newspaper cutout images exist only in the site's `legislation-papers`
 * DOM. Enacting through the site's own tokenised form gives one authoritative response with
 * the talking point, trends and stack artwork together.
 */
object IssueHtmlParser {

    fun parseEnactForm(
        html: String,
        nationId: String,
        issueId: Int,
        optionId: Int,
    ): Outcome<IssueEnactForm> {
        val loggedInNation = loggedInNationId(html)
            ?: return Outcome.Failure(CivilyError.Unauthorized)
        if (loggedInNation != nationId) return malformed("issue page nation mismatch")

        val choiceName = "choice-$optionId"
        val form = formBlocks(html)
            .firstOrNull { form ->
                form.method.equals(POST, ignoreCase = true) &&
                    form.action.contains("page=enact_dilemma") &&
                    form.tags.any { it.isSubmitControl(choiceName) }
            }
            ?: return malformed("issue enact form missing")

        val fields = linkedMapOf<String, String>()
        form.tags
            .filter { it.name == INPUT }
            .forEach { input ->
                val name = input.attributes[NAME]
                if (!name.isNullOrBlank() && input.isPostedWithoutClick()) {
                    fields[name] = input.attributes[VALUE].orEmpty()
                }
            }

        val submit = form.tags.first { it.isSubmitControl(choiceName) }
        fields[choiceName] = submit.attributes[VALUE]?.takeIf { it.isNotBlank() } ?: "1"

        val required = listOf("nation", "id", "kind", "asform_token", choiceName)
        val missing = required.firstOrNull { fields[it].isNullOrBlank() }
        if (missing != null) return malformed("issue enact form missing $missing")
        if (NsId.fromName(fields["nation"].orEmpty()) != nationId) {
            return malformed("issue enact form nation mismatch")
        }
        if (fields["id"] != issueId.toString()) return malformed("issue enact form id mismatch")
        if (fields["kind"] != ENACT) return malformed("issue enact form kind mismatch")

        return Outcome.Success(IssueEnactForm(action = form.action, fields = fields))
    }

    private fun loggedInNationId(html: String): String? {
        val body = findNextTag(html, 0, BODY) ?: return null
        if (!body.attributes[ID].orEmpty().equals(LOGGED_IN, ignoreCase = true)) return null
        return body.attributes[DATA_NNAME]?.let(NsId::fromName)
    }

    fun parseResult(html: String): Outcome<IssueResult> {
        val description = talkingPoint(html)
            ?: return malformed("talking point missing")
        val headlines = newspaperStack(html)
            ?: return malformed("legislation-papers block missing")
        if (headlines.isEmpty()) return malformed("legislation-papers has no papers")

        return Outcome.Success(
            IssueResult(
                description = description,
                headlines = headlines,
                rankings = trends(html),
                // The aftermath page has its own markup for reclassifications, policy changes
                // and unlocked banners, and the captured page carries none of them — so there
                // is nothing here to write a selector against. `c=issue` names all four
                // outright and is the path that actually runs (see
                // `IssuesRepository.answerViaSiteOrApi`), so these stay empty rather than being
                // parsed from a guess at the page's shape.
                reclassifications = emptyList(),
                newPolicies = emptyList(),
                canceledPolicies = emptyList(),
                postcards = emptyList(),
            ),
        )
    }

    private fun formBlocks(html: String): List<FormBlock> {
        val blocks = mutableListOf<FormBlock>()
        var index = 0
        while (index < html.length) {
            val tag = nextTag(html, index) ?: break
            if (!tag.closing && tag.name == FORM) {
                val end = matchingEnd(html, tag, FORM) ?: return emptyList()
                val body = html.substring(tag.end, end.start)
                blocks += FormBlock(
                    action = tag.attributes[ACTION].orEmpty(),
                    method = tag.attributes[METHOD].orEmpty(),
                    tags = tags(body),
                )
                index = end.end
            } else {
                index = tag.end
            }
        }
        return blocks
    }

    private fun talkingPoint(html: String): String? {
        val heading = heading(html, "The Talking Point") ?: return null
        val paragraph = findNextTag(html, heading.end, P) ?: return null
        val end = nextSectionStart(html, paragraph.end) ?: html.length
        return textContent(html.substring(paragraph.end, end)).normalizeHtmlText()
            .takeIf { it.isNotBlank() }
    }

    private fun heading(html: String, text: String): Tag? {
        var index = 0
        while (index < html.length) {
            val tag = nextTag(html, index) ?: return null
            if (!tag.closing && tag.name == H5) {
                val end = matchingEnd(html, tag, H5) ?: return null
                val actual = textContent(html.substring(tag.end, end.start)).normalizeHtmlText()
                if (actual == text) return end
                index = end.end
            } else {
                index = tag.end
            }
        }
        return null
    }

    private fun nextSectionStart(html: String, from: Int): Int? {
        var index = from
        while (index < html.length) {
            val tag = nextTag(html, index) ?: return null
            if (!tag.closing && tag.name == H5) return tag.start
            index = tag.end
        }
        return null
    }

    private fun newspaperStack(html: String): List<IssueResultHeadline>? {
        val container = blockByClass(html, DIV, CONTAINER_CLASS) ?: return null
        val papers = paperBlocks(container)
        return papers.map { block ->
            parsePaper(block) ?: return null
        }
    }

    private fun parsePaper(block: String): IssueResultHeadline? {
        val headlineBlock = blockByClass(block, DIV, HEADLINE_CLASS) ?: return null
        val headline = textContent(headlineBlock).normalizeHtmlText()
            .takeIf { it.isNotBlank() } ?: return null

        return IssueResultHeadline(text = headline, imageUrls = cutouts(block))
    }

    /**
     * Every cutout this paper prints, in the order the page prints them.
     *
     * The names are the page's own and change with the issue — `i16-1.jpg` on one paper,
     * `y108-1.jpg` on the next — so each `src` is carried through untouched. They are found by
     * the shared `dpaperpic` class rather than the numbered ones because document order is what
     * decides which window a cutout belongs in; a paper that prints one, or three, still reads.
     *
     * A paper without artwork is not a broken page. The headline is the news; losing the whole
     * aftermath over a missing photo would report legislation that succeeded as an unknown.
     */
    private fun cutouts(block: String): List<String> {
        val urls = mutableListOf<String>()
        var index = 0
        while (index < block.length) {
            val tag = nextTag(block, index) ?: break
            if (!tag.closing && tag.name == IMG && tag.hasClass(CUTOUT_CLASS)) {
                tag.attributes[SRC]?.let(NsUrl::siteImage)?.let { urls += it }
            }
            index = tag.end
        }
        return urls
    }

    private fun trends(html: String): List<CensusChange> {
        val container = blockByClass(html, DIV, TRENDS_CLASS) ?: return emptyList()
        return anchorBlocks(container)
            .mapNotNull { block ->
                val open = nextTag(block, 0) ?: return@mapNotNull null
                if (!open.hasClass(TREND_CLASS)) return@mapNotNull null
                val scaleId = censusId(open.attributes[HREF].orEmpty()) ?: return@mapNotNull null
                val percent = trendPercent(block) ?: return@mapNotNull null
                CensusChange(scaleId = scaleId, percentChange = percent)
            }
    }

    private fun trendPercent(block: String): Double? {
        val tag = findTag(block, SPAN, PERCENT_CLASS) ?: return null
        val end = matchingEnd(block, tag, SPAN) ?: return null
        val raw = textContent(block.substring(tag.end, end.start))
            .normalizeHtmlText()
            .removeSuffix("%")
            .toDoubleOrNull()
            ?: return null
        return if (tag.hasClass(NEGATIVE_CLASS)) -raw else raw
    }

    private fun censusId(href: String): Int? {
        val key = "censusid="
        val start = href.indexOf(key)
        if (start < 0) return null
        val valueStart = start + key.length
        var valueEnd = valueStart
        while (valueEnd < href.length && href[valueEnd].isDigit()) valueEnd++
        return href.substring(valueStart, valueEnd).toIntOrNull()
    }

    private fun paperBlocks(container: String): List<String> {
        val blocks = mutableListOf<String>()
        var index = 0
        while (index < container.length) {
            val tag = nextTag(container, index) ?: break
            if (!tag.closing && tag.name == DIV && tag.hasClass(PAPER_CLASS)) {
                val end = matchingEnd(container, tag, DIV) ?: return emptyList()
                blocks += container.substring(tag.start, end.end)
                index = end.end
            } else {
                index = tag.end
            }
        }
        return blocks
    }

    private fun anchorBlocks(container: String): List<String> {
        val blocks = mutableListOf<String>()
        var index = 0
        while (index < container.length) {
            val tag = nextTag(container, index) ?: break
            if (!tag.closing && tag.name == A) {
                val end = matchingEnd(container, tag, A) ?: return emptyList()
                blocks += container.substring(tag.start, end.end)
                index = end.end
            } else {
                index = tag.end
            }
        }
        return blocks
    }

    private fun blockByClass(html: String, name: String, className: String): String? {
        val tag = findTag(html, name, className) ?: return null
        val end = matchingEnd(html, tag, name) ?: return null
        return html.substring(tag.end, end.start)
    }

    private fun findTag(html: String, name: String, className: String): Tag? {
        var index = 0
        while (index < html.length) {
            val tag = nextTag(html, index) ?: return null
            if (!tag.closing && tag.name == name && tag.hasClass(className)) return tag
            index = tag.end
        }
        return null
    }

    private fun findNextTag(html: String, from: Int, name: String): Tag? {
        var index = from
        while (index < html.length) {
            val tag = nextTag(html, index) ?: return null
            if (!tag.closing && tag.name == name) return tag
            index = tag.end
        }
        return null
    }

    private fun matchingEnd(html: String, open: Tag, name: String): Tag? {
        var depth = 1
        var index = open.end
        while (index < html.length) {
            val tag = nextTag(html, index) ?: return null
            if (tag.name == name) {
                if (tag.closing) {
                    depth--
                    if (depth == 0) return tag
                } else if (!tag.selfClosing) {
                    depth++
                }
            }
            index = tag.end
        }
        return null
    }

    private fun tags(html: String): List<Tag> {
        val tags = mutableListOf<Tag>()
        var index = 0
        while (index < html.length) {
            val tag = nextTag(html, index) ?: break
            tags += tag
            index = tag.end
        }
        return tags
    }

    private fun nextTag(html: String, from: Int): Tag? {
        var start = html.indexOf('<', from)
        while (start >= 0) {
            if (html.startsWith("<!--", start)) {
                val commentEnd = html.indexOf("-->", start + 4)
                start = if (commentEnd < 0) -1 else html.indexOf('<', commentEnd + 3)
                continue
            }

            val end = tagEnd(html, start + 1) ?: return null
            val tag = parseTag(html, start, end + 1)
            if (tag != null) return tag
            start = html.indexOf('<', end + 1)
        }
        return null
    }

    private fun tagEnd(html: String, from: Int): Int? {
        var quote: Char? = null
        var index = from
        while (index < html.length) {
            val c = html[index]
            when {
                quote != null && c == quote -> quote = null
                quote == null && (c == '"' || c == '\'') -> quote = c
                quote == null && c == '>' -> return index
            }
            index++
        }
        return null
    }

    private fun parseTag(html: String, start: Int, end: Int): Tag? {
        var index = start + 1
        if (index >= end || html[index] == '!') return null
        val closing = html[index] == '/'
        if (closing) index++
        index = html.skipWhitespace(index, end)

        val nameStart = index
        while (index < end && isNameChar(html[index])) index++
        if (index == nameStart) return null
        val name = html.substring(nameStart, index).lowercase(Locale.US)

        val attributes = mutableMapOf<String, String>()
        var selfClosing = false
        while (index < end) {
            index = html.skipWhitespace(index, end)
            if (index >= end || html[index] == '>') break
            if (html[index] == '/') {
                selfClosing = true
                index++
                continue
            }

            val keyStart = index
            while (index < end && isNameChar(html[index])) index++
            if (index == keyStart) {
                index++
                continue
            }
            val key = html.substring(keyStart, index).lowercase(Locale.US)
            index = html.skipWhitespace(index, end)

            var value = ""
            if (index < end && html[index] == '=') {
                index++
                index = html.skipWhitespace(index, end)
                if (index < end && (html[index] == '"' || html[index] == '\'')) {
                    val quote = html[index]
                    val valueStart = ++index
                    while (index < end && html[index] != quote) index++
                    value = html.substring(valueStart, index.coerceAtMost(end))
                    if (index < end) index++
                } else {
                    val valueStart = index
                    while (index < end && !html[index].isWhitespace() && html[index] != '>') {
                        index++
                    }
                    value = html.substring(valueStart, index)
                }
            }
            attributes[key] = HtmlEntities.decode(value)
        }

        return Tag(
            name = name,
            attributes = attributes,
            closing = closing,
            selfClosing = selfClosing || VOID_TAGS.contains(name),
            start = start,
            end = end,
        )
    }

    private fun textContent(html: String): String {
        val out = StringBuilder()
        var index = 0
        while (index < html.length) {
            val tag = nextTag(html, index) ?: break
            out.append(html, index, tag.start)
            if (tag.name == BR) out.append(' ')
            index = tag.end
        }
        if (index < html.length) out.append(html, index, html.length)
        return out.toString()
    }

    private fun String.normalizeHtmlText(): String =
        HtmlEntities.decode(this).collapseWhitespace()

    private fun String.collapseWhitespace(): String {
        val out = StringBuilder(length)
        var inWhitespace = false
        trim().forEach { c ->
            if (c.isWhitespace()) {
                if (!inWhitespace) out.append(' ')
                inWhitespace = true
            } else {
                out.append(c)
                inWhitespace = false
            }
        }
        return out.toString()
    }

    private fun String.skipWhitespace(start: Int, end: Int): Int {
        var index = start
        while (index < end && this[index].isWhitespace()) index++
        return index
    }

    private fun isNameChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '-' || c == '_' || c == ':'

    private fun malformed(detail: String): Outcome.Failure =
        Outcome.Failure(CivilyError.Malformed(detail))

    private fun Tag.hasClass(className: String): Boolean =
        attributes[CLASS]?.splitToSequence(' ', '\t', '\r', '\n', '\u000C')
            ?.any { it == className } == true

    private fun Tag.isSubmitControl(controlName: String): Boolean =
        attributes[NAME] == controlName &&
            DISABLED !in attributes &&
            (
                name == BUTTON ||
                    (name == INPUT && attributes[TYPE].orEmpty().lowercase(Locale.US) == SUBMIT)
            )

    private fun Tag.isPostedWithoutClick(): Boolean =
        name == INPUT &&
            DISABLED !in attributes &&
            attributes[TYPE].orEmpty().lowercase(Locale.US) !in CLICK_ONLY_INPUTS

    private data class FormBlock(
        val action: String,
        val method: String,
        val tags: List<Tag>,
    )

    private data class Tag(
        val name: String,
        val attributes: Map<String, String>,
        val closing: Boolean,
        val selfClosing: Boolean,
        val start: Int,
        val end: Int,
    )

    private const val A = "a"
    private const val ACTION = "action"
    private const val BODY = "body"
    private const val BUTTON = "button"
    private const val CLASS = "class"
    private const val DATA_NNAME = "data-nname"
    private const val DIV = "div"
    private const val DISABLED = "disabled"
    private const val ENACT = "enact"
    private const val FORM = "form"
    private const val H5 = "h5"
    private const val HREF = "href"
    private const val ID = "id"
    private const val IMG = "img"
    private const val INPUT = "input"
    private const val METHOD = "method"
    private const val NAME = "name"
    private const val P = "p"
    private const val POST = "post"
    private const val LOGGED_IN = "loggedin"
    private const val SPAN = "span"
    private const val SRC = "src"
    private const val SUBMIT = "submit"
    private const val TYPE = "type"
    private const val VALUE = "value"
    private const val BR = "br"
    private const val CONTAINER_CLASS = "legislation-papers"
    private const val PAPER_CLASS = "dilemmapaper"
    private const val HEADLINE_CLASS = "dpaper4"
    private const val CUTOUT_CLASS = "dpaperpic"
    private const val TRENDS_CLASS = "wceffects"
    private const val TREND_CLASS = "wc-change"
    private const val PERCENT_CLASS = "wc2"
    private const val NEGATIVE_CLASS = "wcr"

    private val VOID_TAGS = setOf("area", "base", "br", "col", "embed", "hr", "img", "input")
    private val CLICK_ONLY_INPUTS = setOf("submit", "button", "reset", "image")
}

data class IssueEnactForm(
    val action: String,
    val fields: Map<String, String>,
)
