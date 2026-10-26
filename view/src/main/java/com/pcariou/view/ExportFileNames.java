package com.pcariou.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Suggested names for generated SEPA Credit Transfer XML files:
 * {@code yyyyMMdd_HHmmss_PAIN001.xml}.
 *
 * <p>The pattern is bank-neutral: letters, digits and underscores only, at most
 * 50 characters, with an {@code .xml} extension. It is only a suggestion; users
 * can still rename the file.
 */
public final class ExportFileNames
{
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private ExportFileNames()
	{
	}

	/** Suggested name for a file generated now. */
	public static String suggested()
	{
		return suggested(LocalDateTime.now());
	}

	public static String suggested(LocalDateTime time)
	{
		return time.format(STAMP) + "_PAIN001.xml";
	}
}
