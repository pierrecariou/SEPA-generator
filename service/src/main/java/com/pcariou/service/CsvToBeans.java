package com.pcariou.service;

import com.pcariou.model.*;
import java.io.FileReader;

import com.opencsv.bean.*;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;

import java.util.*;

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
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	public Document read(String inputFile) {
		try {
			CsvToBeanBuilder<CreditTransferTransactionInformation> CsvToBeanBuilder = new CsvToBeanBuilder<CreditTransferTransactionInformation>(new FileReader(inputFile));
			CsvToBeanBuilder.withType(CreditTransferTransactionInformation.class);
			CsvToBeanBuilder.withIgnoreLeadingWhiteSpace(true);
			CsvToBeanBuilder.withThrowExceptions(true);
			//CsvToBeanBuilder.withExceptionHandler(new CsvExceptionHandler(exception -> {
			//	System.out.println(exception.getMessage());
			//}));
			List<CreditTransferTransactionInformation> creditTransferTransactionInformations = CsvToBeanBuilder.build().parse();
			for (CreditTransferTransactionInformation creditTransferTransactionInformation : creditTransferTransactionInformations) {
				if (!validate(creditTransferTransactionInformation)) {
					System.out.println("Validation failed");
					return null;
				}
			}
			return createDocument(creditTransferTransactionInformations);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	private Document createDocument(List<CreditTransferTransactionInformation> creditTransferTransactionInformations) {
		DebtorInformations debtorInformations = new DebtorInformations();

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
		
		ProprietaryIdentification proprietaryIdentification = new ProprietaryIdentification("SIRET");
		OrganisationIdentification organisationIdentification = new OrganisationIdentification(proprietaryIdentification);
		PartyIdentification partyIdentification = new PartyIdentification(organisationIdentification);
		InitiatingParty initiatingParty = new InitiatingParty("name", partyIdentification);


		GroupHeader groupHeader = new GroupHeader("messageIdentification", creationDateTime, numberOfTransactions, controlSum, initiatingParty);

		// Payment Information
		PaymentTypeInformation paymentTypeInformation = new PaymentTypeInformation(new ServiceLevel());
		Debtor debtor = new Debtor(debtorInformations.name);
		DebtorAccount debtorAccount = new DebtorAccount( new AccountIdentification(debtorInformations.iban));
		DebtorAgent debtorAgent = new DebtorAgent(new FinancialInstitutionIdentification(debtorInformations.bic));
		PaymentInformation paymentInformation = new PaymentInformation("ID", paymentTypeInformation, "someDate", debtor, debtorAccount, debtorAgent, creditTransferTransactionInformations);

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
