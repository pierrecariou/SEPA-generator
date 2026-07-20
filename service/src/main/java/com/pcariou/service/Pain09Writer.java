package com.pcariou.service;

import com.pcariou.model.CreditTransferTransactionInformation;
import com.pcariou.model.Document;
import com.pcariou.model.GroupHeader;
import com.pcariou.model.InitiatingParty;
import com.pcariou.model.PaymentInformation;
import com.pcariou.model.PostalAddress;
import com.pcariou.model.pain09.Document09;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * pain.001.001.09 writer: maps the parsed/validated shared model to the
 * output-only .09 DTOs ({@link Document09}) and marshals them.
 *
 * <p>Key differences from .02 handled here:
 * <ul>
 *   <li>root child is {@code <CstmrCdtTrfInitn>} (instead of {@code <pain.001.001.02>})</li>
 *   <li>no {@code <Grpg>} element</li>
 *   <li>{@code <BICFI>} instead of {@code <BIC>}</li>
 *   <li>{@code <ReqdExctnDt>} wraps the date in {@code <Dt>}</li>
 *   <li>proprietary ids (SIRET) move from {@code OrgId/PrtryId} to {@code OrgId/Othr/Id}</li>
 *   <li>optional structured {@code <PstlAdr>} on debtor and creditor
 *       (omitted entirely when no address fields were provided)</li>
 * </ul>
 */
public class Pain09Writer implements PainWriter
{
	@Override
	public void write(Document document, String outputFile) throws Exception
	{
		Document09 document09 = map(document);

		SepaXmlMarshaller.marshal(document09, outputFile);
	}

	// ── Mapping (shared .02 model -> .09 DTOs) ───────────────────────────────

	static Document09 map(Document document)
	{
		List<Document09.PaymentInformation> payments = new ArrayList<Document09.PaymentInformation>();
		for (PaymentInformation paymentInformation : document.getPain().getPaymentInformation()) {
			payments.add(mapPaymentInformation(paymentInformation));
		}
		return new Document09(new Document09.CustomerCreditTransferInitiation(
				mapGroupHeader(document.getPain().getGroupHeader()), payments));
	}

	private static Document09.GroupHeader mapGroupHeader(GroupHeader source)
	{
		return new Document09.GroupHeader(
				source.getMessageIdentification(),
				source.getCreationDateTime(),
				source.getNumberOfTransactions(),
				source.getControlSum(),
				mapInitiatingParty(source.getInitiatingParty()));
	}

	private static Document09.Party mapInitiatingParty(InitiatingParty source)
	{
		String proprietaryId = extractProprietaryId(source);
		if (proprietaryId == null) {
			return new Document09.Party(source.getName());
		}
		return new Document09.Party(source.getName(),
				new Document09.PartyIdentification(
						new Document09.OrganisationIdentification(
								new Document09.GenericIdentification(proprietaryId))));
	}

	private static String extractProprietaryId(InitiatingParty source)
	{
		if (source.getPartyIdentification() == null
				|| source.getPartyIdentification().getOrganisationIdentification() == null
				|| source.getPartyIdentification().getOrganisationIdentification().getProprietaryIdentification() == null) {
			return null;
		}
		return source.getPartyIdentification().getOrganisationIdentification().getProprietaryIdentification().getId();
	}

	private static Document09.PaymentInformation mapPaymentInformation(PaymentInformation source)
	{
		List<Document09.CreditTransferTransaction> transactions = new ArrayList<Document09.CreditTransferTransaction>();
		for (CreditTransferTransactionInformation transaction : source.getCreditTransferTransactionInformation()) {
			transactions.add(mapTransaction(transaction));
		}

		return new Document09.PaymentInformation(
				source.getPaymentInformationIdentification(),
				new Document09.PaymentTypeInformation(),
				new Document09.DateAndDateTimeChoice(source.getRequestedExecutionDate()),
				mapParty(source.getDebtor().getName(), source.getDebtor().getPostalAddress()),
				new Document09.Account(source.getDebtorAccount().getAccountIdentification().getIban()),
				new Document09.Agent(source.getDebtorAgent().getFinancialInstitutionIdentification().getBic()),
				transactions);
	}

	private static Document09.CreditTransferTransaction mapTransaction(CreditTransferTransactionInformation source)
	{
		Document09.RemittanceInformation remittance = null;
		if (source.getRemittanceInformation() != null
				&& source.getRemittanceInformation().getUnstructured() != null) {
			remittance = new Document09.RemittanceInformation(source.getRemittanceInformation().getUnstructured());
		}

		return new Document09.CreditTransferTransaction(
				new Document09.PaymentIdentification(source.getPaymentIdentification().getEndToEndIdentification()),
				new Document09.AmountType(new Document09.InstructedAmount(
						source.getAmount().getInstructedAmount().getInstructedAmount(),
						source.getAmount().getInstructedAmount().getCurrency())),
				new Document09.Agent(source.getCreditorAgent().getFinancialInstitutionIdentification().getBic()),
				mapParty(source.getCreditor().getName(), source.getCreditor().getPostalAddress()),
				new Document09.Account(source.getCreditorAccount().getAccountIdentification().getIban()),
				remittance);
	}

	/** Builds a party, attaching a structured address only when one was provided. */
	private static Document09.Party mapParty(String name, PostalAddress address)
	{
		Document09.PostalAddress mapped = mapPostalAddress(address);
		return mapped == null ? new Document09.Party(name) : new Document09.Party(name, mapped);
	}

	private static Document09.PostalAddress mapPostalAddress(PostalAddress address)
	{
		if (address == null || address.isEmpty()) {
			return null;
		}
		return new Document09.PostalAddress(
				clean(address.getStreet()),
				clean(address.getBuildingNumber()),
				clean(address.getPostcode()),
				clean(address.getTown()),
				cleanCountry(address.getCountry()));
	}

	private static String clean(String value)
	{
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String cleanCountry(String value)
	{
		String cleaned = clean(value);
		return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
	}
}
