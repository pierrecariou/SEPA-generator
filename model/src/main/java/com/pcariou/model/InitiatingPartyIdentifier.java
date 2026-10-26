package com.pcariou.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Optional organisation identification of the Group Header initiating party,
 * written as {@code InitgPty/Id/OrgId/Othr}: the identifier value, an optional
 * scheme given either as an ISO code ({@code SchmeNm/Cd}, e.g. {@code BANK},
 * {@code CUST}, {@code TXID}) or as a proprietary name ({@code SchmeNm/Prtry}),
 * and an optional issuer ({@code Issr}).
 *
 * <p>Typical values are a French SIRET, a national company or tax number, or an
 * originator identification number assigned by the customer's bank. This is
 * distinct from the SEPA Creditor Identifier written in {@code CdtrSchmeId}.
 * pain.001.001.02 can carry the identifier and issuer but has no scheme
 * element ({@code PrtryId}), so a scheme is omitted from .02 output.
 *
 * <p>Immutable. Values are trimmed, blank values become {@code null} and the
 * scheme code is upper-cased. Business rules live in {@link #problems()} so
 * profile editors, the profile store and generation share one source of truth.
 */
public final class InitiatingPartyIdentifier
{
	public static final int MAX_TEXT_LENGTH = 35;

	private static final String SCHEME_CODE_PATTERN = "^[A-Z0-9]{1,4}$";
	/** ISO 20022 external organisation identification code for a French SIRET. */
	private static final String SIRET_SCHEME_CODE = "SRET";
	/** ISO 20022 external organisation identification code for a French SIREN. */
	private static final String SIREN_SCHEME_CODE = "SREN";

	private final String id;
	private final String schemeCode;
	private final String schemeProprietary;
	private final String issuer;

	private InitiatingPartyIdentifier(String id, String schemeCode, String schemeProprietary,
			String issuer)
	{
		this.id = id;
		this.schemeCode = schemeCode;
		this.schemeProprietary = schemeProprietary;
		this.issuer = issuer;
	}

	/**
	 * Builds an identifier from raw (possibly blank) values, or returns
	 * {@code null} when every value is blank: the initiating party then carries
	 * its name only.
	 */
	public static InitiatingPartyIdentifier of(String id, String schemeCode,
			String schemeProprietary, String issuer)
	{
		String cleanId = trimToNull(id);
		String cleanCode = trimToNull(schemeCode);
		String cleanProprietary = trimToNull(schemeProprietary);
		String cleanIssuer = trimToNull(issuer);
		if (cleanId == null && cleanCode == null && cleanProprietary == null && cleanIssuer == null) {
			return null;
		}
		return new InitiatingPartyIdentifier(cleanId,
				cleanCode == null ? null : cleanCode.toUpperCase(Locale.ROOT),
				cleanProprietary, cleanIssuer);
	}

	/** An identifier value without scheme or issuer (the historical SIRET shape). */
	public static InitiatingPartyIdentifier ofId(String id)
	{
		return of(id, null, null, null);
	}

	public String getId()
	{
		return id;
	}

	/** The ISO scheme code ({@code SchmeNm/Cd}), or {@code null}. */
	public String getSchemeCode()
	{
		return schemeCode;
	}

	/** The proprietary scheme name ({@code SchmeNm/Prtry}), or {@code null}. */
	public String getSchemeProprietary()
	{
		return schemeProprietary;
	}

	public String getIssuer()
	{
		return issuer;
	}

	/** Whether a scheme name (ISO code or proprietary) is configured. */
	public boolean hasScheme()
	{
		return schemeCode != null || schemeProprietary != null;
	}

	/**
	 * Warning for pain.001.001.02, whose {@code PrtryId} has no scheme-name element,
	 * so a configured scheme cannot be written. Shown wherever that format is chosen.
	 */
	public static final String SCHEME_NOT_IN_PAIN_001_02 =
			"pain.001.001.02 cannot carry the initiating party identifier scheme name, so it is left "
			+ "out of this file (the identifier itself is kept). Choose pain.001.001.03 or .09 if your "
			+ "bank needs the scheme.";

	/**
	 * The nonblocking warning for writing {@code identifier} as {@code version},
	 * or {@code null} when the format can carry everything configured.
	 */
	public static String formatWarning(InitiatingPartyIdentifier identifier, PainVersion version)
	{
		return identifier != null && identifier.hasScheme() && version == PainVersion.PAIN_001_001_02
				? SCHEME_NOT_IN_PAIN_001_02
				: null;
	}

	/** Business-readable problems; empty when the identifier can be written. */
	public List<String> problems()
	{
		List<String> problems = new ArrayList<String>();
		if (id == null) {
			problems.add("The initiating party identifier is required when a scheme or issuer is provided.");
		} else if (id.length() > MAX_TEXT_LENGTH) {
			problems.add("The initiating party identifier must be at most " + MAX_TEXT_LENGTH
					+ " characters.");
		}
		if (schemeCode != null && schemeProprietary != null) {
			problems.add("Use either an ISO scheme code or a proprietary scheme name for the "
					+ "initiating party identifier, not both.");
		}
		if (schemeCode != null && !schemeCode.matches(SCHEME_CODE_PATTERN)) {
			problems.add("The initiating party scheme code must be 1 to 4 letters or digits "
					+ "(e.g. BANK, CUST, TXID).");
		}
		if (schemeProprietary != null && schemeProprietary.length() > MAX_TEXT_LENGTH) {
			problems.add("The initiating party scheme name must be at most " + MAX_TEXT_LENGTH
					+ " characters.");
		}
		if (issuer != null && issuer.length() > MAX_TEXT_LENGTH) {
			problems.add("The initiating party identifier issuer must be at most "
					+ MAX_TEXT_LENGTH + " characters.");
		}
		if (id != null && SIRET_SCHEME_CODE.equals(schemeCode) && !id.matches("^[0-9]{14}$")) {
			problems.add("A SIRET (scheme code SRET) must be exactly 14 digits.");
		}
		if (id != null && SIREN_SCHEME_CODE.equals(schemeCode) && !id.matches("^[0-9]{9}$")) {
			problems.add("A SIREN (scheme code SREN) must be exactly 9 digits.");
		}
		return Collections.unmodifiableList(problems);
	}

	private static String trimToNull(String value)
	{
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other) {
			return true;
		}
		if (!(other instanceof InitiatingPartyIdentifier)) {
			return false;
		}
		InitiatingPartyIdentifier that = (InitiatingPartyIdentifier) other;
		return equal(id, that.id) && equal(schemeCode, that.schemeCode)
				&& equal(schemeProprietary, that.schemeProprietary) && equal(issuer, that.issuer);
	}

	@Override
	public int hashCode()
	{
		return java.util.Arrays.hashCode(new Object[] { id, schemeCode, schemeProprietary, issuer });
	}

	private static boolean equal(String a, String b)
	{
		return a == null ? b == null : a.equals(b);
	}

	@Override
	public String toString()
	{
		return "InitiatingPartyIdentifier{id=" + id + ", schemeCode=" + schemeCode
				+ ", schemeProprietary=" + schemeProprietary + ", issuer=" + issuer + "}";
	}
}
