package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DebtorAgent
{
	@XmlElement(name = "FinInstnId", required = true)
	@NotNull(message = "FinancialInstitutionIdentification for DebtorAgent is mandatory")
	private FinancialInstitutionIdentification financialInstitutionIdentification;

	public DebtorAgent()
	{
	}

	public DebtorAgent(FinancialInstitutionIdentification financialInstitutionIdentification)
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
