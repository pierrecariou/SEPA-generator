package com.pcariou.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link XmlCharacters}, the XML 1.0 legality classifier used to
 * block generation of files that could not be written as well-formed XML.
 */
public class XmlCharactersTest
{
	@Test
	public void ordinaryAsciiIsLegal()
	{
		assertNull(XmlCharacters.firstIllegalChar("INV-2026-001 Payment"));
	}

	@Test
	public void latin1AccentsAreLegal()
	{
		assertNull(XmlCharacters.firstIllegalChar("François Müller Núñez àèìòù"));
	}

	@Test
	public void validNonLatinUnicodeIsLegal()
	{
		assertNull(XmlCharacters.firstIllegalChar("Acme \u4E2D\u56FD Ltd"));
		assertNull(XmlCharacters.firstIllegalChar("Price 10\u20AC"));
	}

	@Test
	public void tabLineFeedAndCarriageReturnAreLegal()
	{
		assertNull(XmlCharacters.firstIllegalChar("line1\tcol"));
		assertNull(XmlCharacters.firstIllegalChar("line1\nline2"));
		assertNull(XmlCharacters.firstIllegalChar("line1\r\nline2"));
	}

	@Test
	public void nullAndEmptyAreLegal()
	{
		assertNull(XmlCharacters.firstIllegalChar(null));
		assertNull(XmlCharacters.firstIllegalChar(""));
	}

	@Test
	public void c0ControlCharactersAreIllegal()
	{
		assertEquals("U+0000", XmlCharacters.firstIllegalChar("A\u0000B"));
		assertEquals("U+0007", XmlCharacters.firstIllegalChar("A\u0007B"));
		assertEquals("U+001F", XmlCharacters.firstIllegalChar("A\u001FB"));
		assertEquals("U+000B", XmlCharacters.firstIllegalChar("A\u000BB")); // vertical tab
		assertEquals("U+000C", XmlCharacters.firstIllegalChar("A\u000CB")); // form feed
	}

	@Test
	public void c1ControlCharactersAreLegalUnderXml10()
	{
		// XML 1.0 permits [#x20-#xD7FF], which includes the C1 range.
		assertNull(XmlCharacters.firstIllegalChar("A\u0085B"));
		assertNull(XmlCharacters.firstIllegalChar("A\u009FB"));
	}

	@Test
	public void nonCharactersFFFEandFFFFareIllegal()
	{
		assertEquals("U+FFFE", XmlCharacters.firstIllegalChar("A\uFFFEB"));
		assertEquals("U+FFFF", XmlCharacters.firstIllegalChar("A\uFFFFB"));
	}

	@Test
	public void validSurrogatePairIsLegal()
	{
		assertNull(XmlCharacters.firstIllegalChar("emoji \uD83D\uDE00 here"));
	}

	@Test
	public void unpairedHighSurrogateIsIllegal()
	{
		String value = "A" + '\uD83D' + "B"; // high surrogate with no following low surrogate
		assertEquals("U+D83D", XmlCharacters.firstIllegalChar(value));
	}

	@Test
	public void unpairedLowSurrogateIsIllegal()
	{
		String value = "A" + '\uDE00' + "B"; // low surrogate with no preceding high surrogate
		assertEquals("U+DE00", XmlCharacters.firstIllegalChar(value));
	}

	@Test
	public void highSurrogateAtEndOfStringIsIllegal()
	{
		assertEquals("U+D83D", XmlCharacters.firstIllegalChar("trailing \uD83D"));
	}

	@Test
	public void firstIllegalCharacterIsReported()
	{
		assertEquals("U+0007", XmlCharacters.firstIllegalChar("ok\u0007\u0001more"));
	}

	@Test
	public void codePointPredicateMatchesTheProduction()
	{
		assertTrue(XmlCharacters.isLegalXml10CodePoint(0x9));
		assertTrue(XmlCharacters.isLegalXml10CodePoint(0xA));
		assertTrue(XmlCharacters.isLegalXml10CodePoint(0xD));
		assertTrue(XmlCharacters.isLegalXml10CodePoint(0x20));
		assertTrue(XmlCharacters.isLegalXml10CodePoint(0x10000));
		assertFalse(XmlCharacters.isLegalXml10CodePoint(0x0));
		assertFalse(XmlCharacters.isLegalXml10CodePoint(0x8));
		assertFalse(XmlCharacters.isLegalXml10CodePoint(0xB));
		assertFalse(XmlCharacters.isLegalXml10CodePoint(0xFFFE));
		assertFalse(XmlCharacters.isLegalXml10CodePoint(0xD800));
	}
}
