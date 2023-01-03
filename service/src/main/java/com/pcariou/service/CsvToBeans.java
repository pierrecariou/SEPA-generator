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

	public CsvToBeans()
	{
		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	public Document read(String inputFile) {
		try {
			CsvToBeanBuilder<CreditTransferTransactionInformation> CsvToBeanBuilder = new CsvToBeanBuilder<CreditTransferTransactionInformation>(new FileReader(inputFile));
			CsvToBeanBuilder.withType(CreditTransferTransactionInformation.class);
			CsvToBeanBuilder.withIgnoreLeadingWhiteSpace(true);
			CsvToBeanBuilder.withThrowExceptions(true);
			List<CreditTransferTransactionInformation> creditTransferTransactionInformations = CsvToBeanBuilder.build().parse();
			for (CreditTransferTransactionInformation creditTransferTransactionInformation : creditTransferTransactionInformations) {
				if (!validate(creditTransferTransactionInformation)) {
					System.out.println("Validation failed: Please check and modify your CSV / Excel file");
					System.exit(1);
				}
			}
			return createDocument(creditTransferTransactionInformations, inputFile);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		return null;
	}

	private Document createDocument(List<CreditTransferTransactionInformation> creditTransferTransactionInformations, String inputFile) {
		DebtorInformations debtorInformations = new DebtorInformations();
		if (!validate(debtorInformations)) {
			System.out.println("Validation failed: Please check and modify your JSON file");
			System.exit(1);
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
		String controlSum = Double.toString(totalAmount);
		
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

		return document;
	}

	private boolean validate(Object object) {
		Set<ConstraintViolation<Object>> violations = validator.validate(object);
		if (violations.size() > 0) {
			for (ConstraintViolation<Object> violation : violations) {
				System.out.println(violation.getMessage());
			}
			return false;
		}
		return true;
	}
}
