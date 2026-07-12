package com.pcariou.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds unique, human-readable ISO 20022 message identifiers for the
 * credit-transfer pipeline, shaped as
 * {@code <PREFIX>-yyyyMMddTHHmmss-XXXXXX} (25 chars for a 2-letter prefix,
 * well within the ISO 20022 35-character Max35Text limit and using only safe
 * text characters).
 *
 * <p>The timestamp keeps the value readable and roughly ordered; the random
 * hexadecimal suffix guarantees uniqueness even for repeated generations of
 * the same input file within the same second. It deliberately does not use
 * the input filename, which is neither unique across runs nor guaranteed to
 * fit Max35Text.
 */
public final class MessageIdGenerator
{
	private static final DateTimeFormatter TIMESTAMP =
			DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
	private static final SecureRandom RANDOM = new SecureRandom();

	private MessageIdGenerator()
	{
	}

	/** Generates a unique message id, e.g. {@code CT-20260712T172341-A7F3C9}. */
	public static String generate(String prefix)
	{
		String timestamp = LocalDateTime.now().format(TIMESTAMP);
		String random = String.format("%06X", RANDOM.nextInt(0x1000000));
		return prefix + "-" + timestamp + "-" + random;
	}
}
