package com.pcariou.model;

/**
 * Supported ISO 20022 pain.001 message versions.
 *
 * <p>{@link #PAIN_001_001_02} is the historical default and must remain the
 * fallback everywhere so existing flows are unchanged.
 */
public enum PainVersion
{
	PAIN_001_001_02("02", "pain.001.001.02"),
	PAIN_001_001_09("09", "pain.001.001.09");

	private final String code;
	private final String schemaId;

	PainVersion(String code, String schemaId)
	{
		this.code = code;
		this.schemaId = schemaId;
	}

	/** Short code used for persistence and the CLI (e.g. {@code "02"}, {@code "09"}). */
	public String getCode()
	{
		return code;
	}

	/** Full schema identifier (e.g. {@code "pain.001.001.09"}). */
	public String getSchemaId()
	{
		return schemaId;
	}

	/**
	 * Resolves a version from its short code or full schema id,
	 * or {@code null} when unknown. Callers decide the fallback.
	 */
	public static PainVersion fromCode(String code)
	{
		if (code == null) {
			return null;
		}
		String trimmed = code.trim();
		for (PainVersion version : values()) {
			if (version.code.equals(trimmed) || version.schemaId.equalsIgnoreCase(trimmed)) {
				return version;
			}
		}
		return null;
	}
}
