package dev.cazh0.stately.data

import android.util.Log
import dev.cazh0.stately.core.result.Outcome
import dev.cazh0.stately.core.result.StatelyError
import dev.cazh0.stately.core.text.NsText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.serialization.XML

/**
 * The one parse boundary in the app.
 *
 * Why a single helper: spec §5 permits catching at parse boundaries only, and requires every
 * catch to log *and* surface a visible failure. Funnelling every response through [decodeNsXml]
 * means that obligation is discharged once, and a repository cannot get it wrong by omission.
 */
val NsXml: XML = XML {
    // Why: NationStates adds shards and attributes without notice. Ignoring unknown children
    // means a server-side addition degrades to "field absent", never to a crash (§1.2).
    defaultPolicy { ignoreUnknownChildren() }
}

/**
 * Why the body is repaired before it is decoded rather than field by field: the mojibake
 * NationStates serves can appear in any text the site stores — a motto, a factbook, a post —
 * and C1 control characters are meaningless in XML, so fixing them once here covers every
 * field without a DTO having to remember.
 */
inline fun <reified T> decodeNsXml(tag: String, body: String): Outcome<T> = try {
    Outcome.Success(NsXml.decodeFromString<T>(NsText.repair(body)))
} catch (e: SerializationException) {
    Outcome.Failure(malformed(tag, e))
} catch (e: XmlException) {
    Outcome.Failure(malformed(tag, e))
}

/** [detail] reaches the log only; the user sees the generic message on [StatelyError.Malformed]. */
fun malformed(tag: String, cause: Throwable): StatelyError.Malformed {
    Log.w("NsXml", "$tag response did not parse", cause)
    return StatelyError.Malformed(cause.message.orEmpty())
}
