package com.pcariou.service;

import com.pcariou.model.*;
import java.io.File;
import java.io.Reader;

import com.opencsv.bean.*;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

import javax.validation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CSV Reader Service
 *
 */
public class CsvToBeans
{
	private final Validator validator;
	private StringBuilder errors;

	private final LocalDate executionDate;
	private List<String> tableResult;

	public CsvToBeans(LocalDate executionDate)
	{
		this.executionDate = executionDate;

		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	public Document read(String inputFile) throws Exception {
			try (Reader reader = CsvSourceReader.open(new File(inputFile))) {
				return read(reader);
			}
	}

	/**
	 * Reads the transactions from an already-opened CSV stream. Extension point
	 * for callers that prepare their own {@link java.io.Reader}; behaviour is
	 * identical to {@link #read(String)}.
	 */
	public Document read(java.io.Reader reader) throws Exception {
			CsvToBeanBuilder<CreditTransferTransactionInformation> CsvToBeanBuilder = new CsvToBeanBuilder<CreditTransferTransactionInformation>(reader);
			CsvToBeanBuilder.withType(CreditTransferTransactionInformation.class);
			CsvToBeanBuilder.withIgnoreLeadingWhiteSpace(true);
			CsvToBeanBuilder.withThrowExceptions(true);
			List<CreditTransferTransactionInformation> creditTransferTransactionInformations = CsvToBeanBuilder.build().parse();
			for (CreditTransferTransactionInformation creditTransferTransactionInformation : creditTransferTransactionInformations) {
				normalizeRemittanceInformation(creditTransferTransactionInformation);
				normalizeAccountFields(creditTransferTransactionInformation);
			}
			validateRows(creditTransferTransactionInformations);
			return createDocument(creditTransferTransactionInformations);
	}

	/**
	 * Validates every row, collecting all findings with their source row number
	 * (the header is line 1, so the first data row is line 2). This gives
	 * imported-data errors precise row context instead of aborting on the first
	 * offending row without a line number.
	 */
	private void validateRows(List<CreditTransferTransactionInformation> rows) throws Exception
	{
		StringBuilder rowErrors = new StringBuilder();
		for (int i = 0; i < rows.size(); i++) {
			Set<ConstraintViolation<CreditTransferTransactionInformation>> violations =
					validator.validate(rows.get(i));
			for (ConstraintViolation<CreditTransferTransactionInformation> violation : violations) {
				rowErrors.append("Row ").append(i + 2).append(": ")
						.append(violation.getMessage()).append("\n");
			}
		}
		if (rowErrors.length() > 0) {
			throw new Exception("Invalid input file\n" + rowErrors.toString());
		}
	}

	/**
	 * Canonicalises the creditor IBAN and BIC to the exact form that will be
	 * emitted (whitespace removed / upper-cased), so the value validated is the
	 * value written to the file. Purely lexical; never invents account data.
	 */
	private static void normalizeAccountFields(CreditTransferTransactionInformation transaction)
	{
		CreditorAccount account = transaction.getCreditorAccount();
		if (account != null && account.getAccountIdentification() != null) {
			AccountIdentification identification = account.getAccountIdentification();
			identification.setIban(SepaFieldNormalizer.iban(identification.getIban()));
		}
		CreditorAgent agent = transaction.getCreditorAgent();
		if (agent != null && agent.getFinancialInstitutionIdentification() != null) {
			FinancialInstitutionIdentification institution =
					agent.getFinancialInstitutionIdentification();
			institution.setBic(SepaFieldNormalizer.bic(institution.getBic()));
		}
	}

	/**
	 * Drops the RmtInf element entirely when the optional "information" column
	 * is blank: the ISO 20022 schemas require Ustrd, when present, to be a
	 * non-empty Max140Text, so an empty {@code <Ustrd></Ustrd>} would make the
	 * generated file schema-invalid.
	 */
	private static void normalizeRemittanceInformation(CreditTransferTransactionInformation transaction)
	{
		RemittanceInformation remittance = transaction.getRemittanceInformation();
		if (remittance != null
				&& (remittance.getUnstructured() == null || remittance.getUnstructured().trim().isEmpty())) {
			transaction.setRemittanceInformation(null);
		}
	}

	private Document createDocument(List<CreditTransferTransactionInformation> creditTransferTransactionInformations) throws Exception {
		DebtorInformations debtorInformations = new DebtorInformations(executionDate);
		if (!validate(debtorInformations)) {
			throw new Exception("Validation failed: Please check and modify your JSON file\n" + this.errors.toString());
		}

		// Group Header
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		String creationDateTime = now.format(formatter);

		String numberOfTransactions = Integer.toString(creditTransferTransactionInformations.size());

		java.math.BigDecimal totalAmountDecimal = java.math.BigDecimal.ZERO;
		for (CreditTransferTransactionInformation creditTransferTransactionInformation : creditTransferTransactionInformations) {
			InstructedAmount instructedAmount = creditTransferTransactionInformation.getAmount().getInstructedAmount();
			java.math.BigDecimal amount = new java.math.BigDecimal(normalizedAmount(instructedAmount.getInstructedAmount()));
			instructedAmount.setInstructedAmount(String.format(Locale.US, "%.2f", amount));
			totalAmountDecimal = totalAmountDecimal.add(amount);
		}
		String controlSum = String.format(Locale.US,"%.2f", totalAmountDecimal);
		
		ProprietaryIdentification proprietaryIdentification = new ProprietaryIdentification(debtorInformations.initiatingPartySiret);
		OrganisationIdentification organisationIdentification = new OrganisationIdentification(proprietaryIdentification);
		PartyIdentification partyIdentification = new PartyIdentification(organisationIdentification);
		InitiatingParty initiatingParty = new InitiatingParty(debtorInformations.initiatingPartyName, partyIdentification);

		// Unique MsgId/PmtInfId: filenames are neither unique nor guaranteed
		// to fit the ISO 20022 Max35Text limit (schema-invalid otherwise).
		String messageId = MessageIdGenerator.generate("CT");

		GroupHeader groupHeader = new GroupHeader(messageId, creationDateTime, numberOfTransactions, controlSum, initiatingParty);

		// Payment Information
		PaymentTypeInformation paymentTypeInformation = new PaymentTypeInformation(new ServiceLevel());
		Debtor debtor = new Debtor(debtorInformations.name);
		debtor.setPostalAddress(debtorInformations.address);
		DebtorAccount debtorAccount = new DebtorAccount( new AccountIdentification(debtorInformations.iban));
		DebtorAgent debtorAgent = new DebtorAgent(new FinancialInstitutionIdentification(debtorInformations.bic));
		PaymentInformation paymentInformation = new PaymentInformation(messageId + "-1", paymentTypeInformation, debtorInformations.requestedExecutionDate, debtor, debtorAccount, debtorAgent, creditTransferTransactionInformations);

		// Document
		ArrayList<PaymentInformation> paymentInformations = new ArrayList<>();
		paymentInformations.add(paymentInformation);
		Pain pain = new Pain(groupHeader, paymentInformations);
		Document document = new Document(pain);

		tableResult = new ArrayList<>();
		tableResult.add(numberOfTransactions);
		tableResult.add(controlSum);
		tableResult.add(debtorInformations.requestedExecutionDate);

		return document;
	}

	public List<String> getTableResult() {
		return tableResult;
	}

	/** Accepts "," as decimal separator, matching the amount validation. */
	private static String normalizedAmount(String amount)
	{
		return amount.trim().replace(',', '.');
	}

	private boolean validate(Object object) {
		Set<ConstraintViolation<Object>> violations = validator.validate(object);
		this.errors = new StringBuilder();
		if (violations.size() > 0) {
			for (ConstraintViolation<Object> violation : violations) {
				errors.append(violation.getMessage() + "\n");
			}
			return false;
		}
		return true;
	}
}
