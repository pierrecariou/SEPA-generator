package com.pcariou.service;

import com.pcariou.model.*;
import java.io.FileReader;

import com.opencsv.bean.*;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;

import java.util.*;

import javax.validation.*;

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
		GroupHeader groupHeader = new GroupHeader();

		Debtor debtor = new Debtor();
		DebtorAccount debtorAccount = new DebtorAccount();
		DebtorAgent debtorAgent = new DebtorAgent();
		PaymentInformation paymentInformation = new PaymentInformation("2023-01-12", debtor, debtorAccount, debtorAgent, creditTransferTransactionInformations);

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
