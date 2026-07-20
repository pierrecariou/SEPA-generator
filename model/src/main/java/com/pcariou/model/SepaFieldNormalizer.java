package com.pcariou.model;

/**
 * Deterministic lexical normalisation for account identifiers, so the value
 * that is validated is exactly the value emitted in the generated XML.
 *
 * <p>This performs only safe, loss-free canonicalisation — removing spaces and
 * upper-casing — which is how IBANs and BICs are defined to be compared. It
 * never invents, completes or replaces account data: a {@code null} stays
 * {@code null} and a blank value is returned unchanged so the mandatory-field
 * rules still apply.
 */
public final class SepaFieldNormalizer
{
	private SepaFieldNormalizer()
	{
	}

	/** Canonical IBAN form: all whitespace removed and upper-cased. */
	public static String iban(String value)
	{
		if (value == null) {
			return null;
		}
		return value.replaceAll("\\s", "").toUpperCase();
	}

	/** Canonical BIC form: trimmed and upper-cased. */
	public static String bic(String value)
	{
		if (value == null) {
			return null;
		}
		return value.trim().toUpperCase();
	}
}
