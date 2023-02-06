package com.pcariou.service;

import com.pcariou.model.*;
import java.io.FileReader;

import com.opencsv.bean.*;

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
	private Validator validator;
	private StringBuilder errors;

	private Date executionDate;
	private List<String> tableResult;

	public CsvToBeans(Date executionDate)
	{
		this.executionDate = executionDate;

		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	public Document read(String inputFile) throws Exception {
			CsvToBeanBuilder<CreditTransferTransactionInformation> CsvToBeanBuilder = new CsvToBeanBuilder<CreditTransferTransactionInformation>(new FileReader(inputFile));
			CsvToBeanBuilder.withType(CreditTransferTransactionInformation.class);
			CsvToBeanBuilder.withIgnoreLeadingWhiteSpace(true);
			CsvToBeanBuilder.withThrowExceptions(true);
			List<CreditTransferTransactionInformation> creditTransferTransactionInformations = CsvToBeanBuilder.build().parse();
			for (CreditTransferTransactionInformation creditTransferTransactionInformation : creditTransferTransactionInformations) {
				if (!validate(creditTransferTransactionInformation)) {
					throw new Exception("Invalid CSV file\n" + this.errors.toString());
				}
			}
			return createDocument(creditTransferTransactionInformations, inputFile);
	}

	private Document createDocument(List<CreditTransferTransactionInformation> creditTransferTransactionInformations, String inputFile) throws Exception {
		DebtorInformations debtorInformations = new DebtorInformations(executionDate);
		if (!validate(debtorInformations)) {
			throw new Exception("Validation failed: Please check and modify your JSON file\n" + this.errors.toString());
		}

		// Group Header
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		String creationDateTime = now.format(formatter);

		String numberOfTransactions = Integer.toString(creditTransferTransactionInformations.size());

		double totalAmount = 0;
		for (CreditTransferTransactionInformation creditTransferTransactionInformation : creditTransferTransactionInformations) {
			totalAmount += Double.valueOf(creditTransferTransactionInformation.getAmount().getInstructedAmount().getInstructedAmount());
		}
		String controlSum = String.format("%.2f", totalAmount);
		
		ProprietaryIdentification proprietaryIdentification = new ProprietaryIdentification(debtorInformations.initiatingPartySiret);
		OrganisationIdentification organisationIdentification = new OrganisationIdentification(proprietaryIdentification);
		PartyIdentification partyIdentification = new PartyIdentification(organisationIdentification);
		InitiatingParty initiatingParty = new InitiatingParty(debtorInformations.initiatingPartyName, partyIdentification);

		GroupHeader groupHeader = new GroupHeader(inputFile, creationDateTime, numberOfTransactions, controlSum, initiatingParty);

		// Payment Information
		PaymentTypeInformation paymentTypeInformation = new PaymentTypeInformation(new ServiceLevel());
		Debtor debtor = new Debtor(debtorInformations.name);
		DebtorAccount debtorAccount = new DebtorAccount( new AccountIdentification(debtorInformations.iban));
		DebtorAgent debtorAgent = new DebtorAgent(new FinancialInstitutionIdentification(debtorInformations.bic));
		PaymentInformation paymentInformation = new PaymentInformation(inputFile + "-1", paymentTypeInformation, debtorInformations.requestedExecutionDate, debtor, debtorAccount, debtorAgent, creditTransferTransactionInformations);

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
