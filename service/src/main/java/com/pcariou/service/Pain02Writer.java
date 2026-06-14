package com.pcariou.service;

import com.pcariou.model.Document;

/**
 * pain.001.001.02 writer: delegates to the existing {@link BeansToXml}
 * path, which remains byte-for-byte unchanged.
 */
public class Pain02Writer implements PainWriter
{
	@Override
	public void write(Document document, String outputFile) throws Exception
	{
		new BeansToXml().write(document, outputFile);
	}
}
