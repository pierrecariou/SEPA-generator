package com.pcariou.service;

import com.pcariou.model.CreditTransferTransactionInformation;
import com.pcariou.model.Document;
import com.pcariou.model.GroupHeader;
import com.pcariou.model.InitiatingParty;
import com.pcariou.model.PaymentInformation;
import com.pcariou.model.PostalAddress;
import com.pcariou.model.pain03.Document03;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * pain.001.001.03 writer: maps the parsed/validated shared model to the
 * output-only .03 DTOs ({@link Document03}) and marshals them.
 *
 * <p>pain.001.001.03 is offered for banks and upload channels that still
 * require the legacy format; pain.001.001.09 remains the modern format. The
 * mapping is deliberately the same conservative SEPA content as .02 and .09 —
 * no extra ISO or national elements are invented for .03.
 *
 * <p>Key differences from .09 handled here:
 * <ul>
 *   <li>{@code <ReqdExctnDt>} carries the date directly (no {@code <Dt>} wrapper)</li>
 *   <li>{@code <BIC>} instead of {@code <BICFI>}</li>
 *   <li>{@code <ChrgBr>SLEV</ChrgBr>} is emitted on the payment block, as in .02</li>
 *   <li>{@code <NbOfTxs>}/{@code <CtrlSum>} are also emitted per payment block,
 *       derived from that block's transactions</li>
 * </ul>
 *
 * <p>Differences from .02: the root child is {@code <CstmrCdtTrfInitn>}, there
 * is no {@code <Grpg>}, proprietary ids (SIRET) move to {@code OrgId/Othr/Id},
 * and structured postal addresses are supported.
 */
public class Pain03Writer implements PainWriter
{
	@Override
	public void write(Document document, String outputFile) throws Exception
	{
		Document03 document03 = map(document);
		SepaXmlMarshaller.marshal(document03, outputFile);
	}

	// ── Mapping (shared .02 model -> .03 DTOs) ───────────────────────────────

	static Document03 map(Document document)
	{
		List<Document03.PaymentInformation> payments = new ArrayList<Document03.PaymentInformation>();
		for (PaymentInformation paymentInformation : document.getPain().getPaymentInformation()) {
			payments.add(mapPaymentInformation(paymentInformation));
		}
		return new Document03(new Document03.CustomerCreditTransferInitiation(
				mapGroupHeader(document.getPain().getGroupHeader()), payments));
	}

	private static Document03.GroupHeader mapGroupHeader(GroupHeader source)
	{
		return new Document03.GroupHeader(
				source.getMessageIdentification(),
				source.getCreationDateTime(),
				source.getNumberOfTransactions(),
				source.getControlSum(),
				mapInitiatingParty(source.getInitiatingParty()));
	}

	private static Document03.Party mapInitiatingParty(InitiatingParty source)
	{
		String proprietaryId = extractProprietaryId(source);
		if (proprietaryId == null) {
			return new Document03.Party(source.getName());
		}
		return new Document03.Party(source.getName(),
				new Document03.PartyIdentification(
						new Document03.OrganisationIdentification(
								new Document03.GenericIdentification(proprietaryId))));
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

	private static Document03.PaymentInformation mapPaymentInformation(PaymentInformation source)
	{
		List<Document03.CreditTransferTransaction> transactions =
				new ArrayList<Document03.CreditTransferTransaction>();
		BigDecimal blockTotal = BigDecimal.ZERO;
		for (CreditTransferTransactionInformation transaction : source.getCreditTransferTransactionInformation()) {
			transactions.add(mapTransaction(transaction));
			blockTotal = blockTotal.add(new BigDecimal(
					transaction.getAmount().getInstructedAmount().getInstructedAmount()));
		}

		return new Document03.PaymentInformation(
				source.getPaymentInformationIdentification(),
				Integer.toString(transactions.size()),
				String.format(Locale.US, "%.2f", blockTotal),
				new Document03.PaymentTypeInformation(),
				source.getRequestedExecutionDate(),
				mapParty(source.getDebtor().getName(), source.getDebtor().getPostalAddress()),
				new Document03.Account(source.getDebtorAccount().getAccountIdentification().getIban()),
				new Document03.Agent(source.getDebtorAgent().getFinancialInstitutionIdentification().getBic()),
				transactions);
	}

	private static Document03.CreditTransferTransaction mapTransaction(CreditTransferTransactionInformation source)
	{
		Document03.RemittanceInformation remittance = null;
		if (source.getRemittanceInformation() != null
				&& source.getRemittanceInformation().getUnstructured() != null) {
			remittance = new Document03.RemittanceInformation(source.getRemittanceInformation().getUnstructured());
		}

		return new Document03.CreditTransferTransaction(
				new Document03.PaymentIdentification(source.getPaymentIdentification().getEndToEndIdentification()),
				new Document03.AmountType(new Document03.InstructedAmount(
						source.getAmount().getInstructedAmount().getInstructedAmount(),
						source.getAmount().getInstructedAmount().getCurrency())),
				new Document03.Agent(source.getCreditorAgent().getFinancialInstitutionIdentification().getBic()),
				mapParty(source.getCreditor().getName(), source.getCreditor().getPostalAddress()),
				new Document03.Account(source.getCreditorAccount().getAccountIdentification().getIban()),
				remittance);
	}

	/** Builds a party, attaching a structured address only when one was provided. */
	private static Document03.Party mapParty(String name, PostalAddress address)
	{
		Document03.PostalAddress mapped = mapPostalAddress(address);
		return mapped == null ? new Document03.Party(name) : new Document03.Party(name, mapped);
	}

	private static Document03.PostalAddress mapPostalAddress(PostalAddress address)
	{
		if (address == null || address.isEmpty()) {
			return null;
		}
		return new Document03.PostalAddress(
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
