package com.pcariou.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Opens a CSV input file with a deterministic, platform-independent decoding:
 * always UTF-8, with a leading UTF-8 byte-order mark stripped so it never
 * reaches the CSV parser (a BOM on the first header cell would otherwise stop
 * the first column from binding).
 *
 * <p>This replaces the previous {@code new FileReader(...)} usage, whose charset
 * was the JVM platform default (e.g. Windows-1252 on a typical Windows
 * installation). That made the same UTF-8 export decode differently depending
 * on the machine, silently corrupting accented names, remittance text and other
 * business-significant content. Centralising the decision here keeps every CSV
 * entry point reading the file the same way.
 *
 * <p>The caller owns the returned reader and must close it.
 */
public final class CsvSourceReader
{
	private CsvSourceReader()
	{
	}

	/** Opens {@code file} as a UTF-8, BOM-stripped character stream. */
	public static Reader open(File file) throws IOException
	{
		InputStreamReader utf8 = new InputStreamReader(
				new FileInputStream(file), StandardCharsets.UTF_8);
		PushbackReader reader = new PushbackReader(utf8, 1);
		int first = reader.read();
		if (first != -1 && first != '\uFEFF') {
			reader.unread(first);
		}
		return reader;
	}

	/** Opens {@code path} as a UTF-8, BOM-stripped character stream. */
	public static Reader open(Path path) throws IOException
	{
		return open(path.toFile());
	}
}
