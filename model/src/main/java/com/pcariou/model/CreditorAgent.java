package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.CsvRecurse;
import javax.validation.Valid;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CreditorAgent
{
	@XmlElement(name = "FinInstnId")
	@NotNull(message = "FinancialInstitutionIdentification for CreditorAgent is mandatory")
	@Valid()
	@CsvRecurse()
	private FinancialInstitutionIdentification financialInstitutionIdentification;

	public CreditorAgent()
	{
	}

	public CreditorAgent(FinancialInstitutionIdentification financialInstitutionIdentification)
	{
		this.financialInstitutionIdentification = financialInstitutionIdentification;
	}

	public FinancialInstitutionIdentification getFinancialInstitutionIdentification()
	{
		return financialInstitutionIdentification;
	}

	public void setFinancialInstitutionIdentification(FinancialInstitutionIdentification financialInstitutionIdentification)
	{
		this.financialInstitutionIdentification = financialInstitutionIdentification;
	}
}
