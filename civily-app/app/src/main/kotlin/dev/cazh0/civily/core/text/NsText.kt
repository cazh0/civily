package dev.cazh0.civily.core.text

/**
 * Repairs mojibake that NationStates itself serves.
 *
 * A good deal of stored content was originally Windows-1252 and was re-encoded as UTF-8 one
 * step too late: the byte `0x92`, a right single quote in Windows-1252, became U+0092 — a C1
 * control character with no glyph. The device draws it as a tofu box, so `doesn’t` reaches the
 * reader as `doesn▯t`.
 *
 * Browsers do not show this because the HTML standard requires them to treat this exact range
 * as Windows-1252. This does the same, for the same reason. Nothing is guessed: C1 controls
 * carry no meaning in XML content, so remapping them can only repair text, never alter
 * something an author wrote.
 */
object NsText {

    fun repair(input: String): String {
        // Cheap rejection: almost every response contains none of these.
        if (input.none { it.code in C1_START..C1_END }) return input

        return buildString(input.length) {
            for (c in input) append(windows1252(c.code) ?: c)
        }
    }

    /**
     * What Windows-1252 puts at a C1 code point, or null for anything it leaves unassigned and
     * anything outside the range.
     *
     * Also used by [HtmlEntities], because a numeric reference in this range — `&#149;` for a
     * bullet — is the same mistake arriving by a different road, and the HTML standard requires
     * browsers to resolve it the same way.
     */
    fun windows1252(code: Int): Char? {
        if (code !in C1_START..C1_END) return null
        return WINDOWS_1252[code - C1_START].takeIf { it.code !in C1_START..C1_END }
    }

    private const val C1_START = 0x80
    private const val C1_END = 0x9F

    /**
     * Windows-1252's assignments for 0x80–0x9F, written as escapes so the source stays
     * readable and cannot be mangled by an editor. The five slots Windows-1252 leaves
     * unassigned map to themselves rather than to an invented character.
     */
    private val WINDOWS_1252 = charArrayOf(
        '€', '', '‚', 'ƒ', // 80 € · 81 unassigned · 82 ‚ · 83 ƒ
        '„', '…', '†', '‡', // 84 „ · 85 … · 86 † · 87 ‡
        'ˆ', '‰', 'Š', '‹', // 88 ˆ · 89 ‰ · 8A Š · 8B ‹
        'Œ', '', 'Ž', '', // 8C Œ · 8D unassigned · 8E Ž · 8F unassigned
        '', '‘', '’', '“', // 90 unassigned · 91 ‘ · 92 ’ · 93 “
        '”', '•', '–', '—', // 94 ” · 95 • · 96 – · 97 —
        '˜', '™', 'š', '›', // 98 ˜ · 99 ™ · 9A š · 9B ›
        'œ', '', 'ž', 'Ÿ', // 9C œ · 9D unassigned · 9E ž · 9F Ÿ
    )
}
