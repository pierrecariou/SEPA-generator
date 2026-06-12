package com.pcariou.service;

import com.pcariou.model.Document;
import com.pcariou.model.PainVersion;

/**
 * Strategy for writing a parsed and validated SEPA document to an XML file
 * in a specific pain.001 version.
 *
 * <p>The shared parse/validation model ({@link com.pcariou.model.Document})
 * is the single input for every version; each implementation owns the
 * version-specific output mapping and marshalling.
 */
public interface PainWriter
{
	void write(Document document, String outputFile) throws Exception;

	/**
	 * Returns the writer for the given version, defaulting to
	 * pain.001.001.02 when {@code version} is {@code null} so existing
	 * call sites keep their historical behavior.
	 */
	static PainWriter forVersion(PainVersion version)
	{
		if (version == PainVersion.PAIN_001_001_09) {
			return new Pain09Writer();
		}
		return new Pain02Writer();
	}
}
