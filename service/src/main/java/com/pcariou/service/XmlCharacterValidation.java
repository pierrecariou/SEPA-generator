package com.pcariou.service;

import com.pcariou.model.CreditTransferTransactionInformation;
import com.pcariou.model.Creditor;
import com.pcariou.model.PostalAddress;
import com.pcariou.model.RemittanceInformation;
import com.pcariou.model.XmlCharacters;

import java.util.ArrayList;
import java.util.List;

/**
 * Core generation-safety scan: finds imported values that contain an XML
 * 1.0-illegal character and therefore cannot be written into a well-formed
 * SEPA file. Generation fails before any XML is written.
 *
 * <p>Only free-text fields are scanned. IBAN, BIC, amount, dates, SIRET and
 * the country code are already constrained to a safe character subset by their
 * own validators, so an XML-illegal character can never reach them.
 *
 * <p>This never mutates the input; character classification is delegated to
 * the pure {@link XmlCharacters} helper.
 */
public final class XmlCharacterValidation
{
	private XmlCharacterValidation()
	{
	}

	/** A single XML-legality problem: the offending field and the illegal character. */
	public static final class Finding
	{
		private final String fieldKey;
		private final String fieldLabel;
		private final String illegalChar;

		Finding(String fieldKey, String fieldLabel, String illegalChar)
		{
			this.fieldKey = fieldKey;
			this.fieldLabel = fieldLabel;
			this.illegalChar = illegalChar;
		}

		/** Diagnostics-oriented field key (matches the CSV column labels). */
		public String getFieldKey()
		{
			return fieldKey;
		}

		public String getIllegalChar()
		{
			return illegalChar;
		}

		/** Business-readable, field-tagged message. */
		public String message()
		{
			return "The " + fieldLabel + " contains a character that is not permitted in XML "
					+ "and cannot be written to a SEPA file: " + illegalChar + ".";
		}
	}

	/**
	 * Scans all Credit Transfer free-text transaction fields that can reach
	 * generated XML: creditor name, end-to-end reference, remittance information,
	 * and creditor address fields.
	 */
	public static List<Finding> scan(CreditTransferTransactionInformation transaction)
	{
		List<Finding> findings = new ArrayList<Finding>();
		if (transaction == null) {
			return findings;
		}
		check(findings, "name", "creditor name", creditorName(transaction));
		check(findings, "end_to_end_id", "end-to-end reference", endToEndReference(transaction));
		check(findings, "information", "remittance information", remittance(transaction));

		PostalAddress address = creditorAddress(transaction);
		if (address != null) {
			check(findings, "street", "creditor street", address.getStreet());
			check(findings, "building_number", "creditor building number", address.getBuildingNumber());
			check(findings, "postcode", "creditor postcode", address.getPostcode());
			check(findings, "town", "creditor town/city", address.getTown());
			check(findings, "country", "creditor country", address.getCountry());
		}
		return findings;
	}

	/**
	 * Scans debtor and initiating-party configuration values that are written
	 * into generated CT XML. IBAN, BIC, SIRET and dates are already constrained
	 * to XML-safe character subsets by their own validators and are not scanned.
	 * The country field of the debtor address is validated as a 2-letter ISO
	 * alpha code and is also omitted.
	 */
	public static List<Finding> scan(DebtorInformations debtor)
	{
		List<Finding> findings = new ArrayList<Finding>();
		if (debtor == null) {
			return findings;
		}
		check(findings, "Debtor name", "debtor name", debtor.name);
		check(findings, "Initiating party name", "initiating party name", debtor.initiatingPartyName);
		if (debtor.address != null) {
			check(findings, "Debtor street", "debtor street", debtor.address.getStreet());
			check(findings, "Debtor building number", "debtor building number", debtor.address.getBuildingNumber());
			check(findings, "Debtor postcode", "debtor postcode", debtor.address.getPostcode());
			check(findings, "Debtor town/city", "debtor town/city", debtor.address.getTown());
		}
		return findings;
	}

	private static void check(List<Finding> findings, String fieldKey, String fieldLabel, String value)
	{
		String illegal = XmlCharacters.firstIllegalChar(value);
		if (illegal != null) {
			findings.add(new Finding(fieldKey, fieldLabel, illegal));
		}
	}

	private static String creditorName(CreditTransferTransactionInformation transaction)
	{
		Creditor creditor = transaction.getCreditor();
		return creditor == null ? null : creditor.getName();
	}

	private static PostalAddress creditorAddress(CreditTransferTransactionInformation transaction)
	{
		Creditor creditor = transaction.getCreditor();
		return creditor == null ? null : creditor.getPostalAddress();
	}

	private static String endToEndReference(CreditTransferTransactionInformation transaction)
	{
		return transaction.getPaymentIdentification() == null
				? null
				: transaction.getPaymentIdentification().getEndToEndIdentification();
	}

	private static String remittance(CreditTransferTransactionInformation transaction)
	{
		RemittanceInformation remittance = transaction.getRemittanceInformation();
		return remittance == null ? null : remittance.getUnstructured();
	}
}
