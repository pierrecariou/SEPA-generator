package com.pcariou.service;

import com.pcariou.model.CreditTransferTransactionInformation;
import com.pcariou.model.Document;
import com.pcariou.model.GroupHeader;
import com.pcariou.model.InitiatingParty;
import com.pcariou.model.PaymentInformation;
import com.pcariou.model.pain09.Document09;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
 * </ul>
 */
public class Pain09Writer implements PainWriter
{
	@Override
	public void write(Document document, String outputFile) throws Exception
	{
		Document09 document09 = map(document);

		JAXBContext context = JAXBContext.newInstance(Document09.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
		marshaller.marshal(document09, new File(outputFile));
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
				new Document09.Party(source.getDebtor().getName()),
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
				new Document09.Party(source.getCreditor().getName()),
				new Document09.Account(source.getCreditorAccount().getAccountIdentification().getIban()),
				remittance);
	}
}
