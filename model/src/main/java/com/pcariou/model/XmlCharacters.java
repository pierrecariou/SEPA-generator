package com.pcariou.model;

/**
 * Core generation-safety helper: classifies whether text is writable as XML 1.0
 * character data.
 *
 * <p>It follows the XML 1.0 {@code Char} production exactly:
 * <pre>
 *   Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
 * </pre>
 * Tab, line feed and carriage return are therefore legal and are never
 * rejected. Everything else in the C0 range (and the lone {@code U+FFFE} /
 * {@code U+FFFF} non-characters) is illegal. Unpaired surrogates — which cannot
 * occur in well-formed XML — are treated as illegal too.
 *
 * <p>The helper only classifies; it never mutates, strips, replaces or
 * transliterates data.
 */
public final class XmlCharacters
{
	private XmlCharacters()
	{
	}

	/** Whether {@code codePoint} is a legal XML 1.0 character. */
	public static boolean isLegalXml10CodePoint(int codePoint)
	{
		return codePoint == 0x9
				|| codePoint == 0xA
				|| codePoint == 0xD
				|| (codePoint >= 0x20 && codePoint <= 0xD7FF)
				|| (codePoint >= 0xE000 && codePoint <= 0xFFFD)
				|| (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
	}

	/**
	 * Returns a display token (e.g. {@code "U+0007"}) for the first XML
	 * 1.0-illegal unit — an illegal code point or an unpaired surrogate — or
	 * {@code null} when the whole value can be written as XML character data.
	 * A {@code null} value is legal.
	 */
	public static String firstIllegalChar(String value)
	{
		if (value == null) {
			return null;
		}
		int length = value.length();
		int i = 0;
		while (i < length) {
			char c = value.charAt(i);
			if (Character.isHighSurrogate(c)) {
				if (i + 1 < length && Character.isLowSurrogate(value.charAt(i + 1))) {
					int codePoint = Character.toCodePoint(c, value.charAt(i + 1));
					if (!isLegalXml10CodePoint(codePoint)) {
						return display(codePoint);
					}
					i += 2;
					continue;
				}
				return display(c); // unpaired high surrogate
			}
			if (Character.isLowSurrogate(c)) {
				return display(c); // unpaired low surrogate
			}
			if (!isLegalXml10CodePoint(c)) {
				return display(c);
			}
			i++;
		}
		return null;
	}

	private static String display(int codePoint)
	{
		return String.format("U+%04X", codePoint);
	}
}
