package com.pcariou.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;

public class ExportFileNamesTest
{
	private static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 26, 14, 52, 0);

	@Test
	public void creditTransfersUseTimestampAndPain001()
	{
		assertEquals("20261026_145200_PAIN001.xml", ExportFileNames.suggested(TIME));
	}

	@Test
	public void namesAreBankNeutral()
	{
		String name = ExportFileNames.suggested();
		assertTrue(name, name.matches("[0-9]{8}_[0-9]{6}_PAIN001\\.xml"));
		assertTrue(name, name.length() <= 50);
	}
}
