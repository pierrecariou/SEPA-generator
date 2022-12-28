package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class FinancialInstitutionIdentification
{
	@XmlElement(name = "BIC")
	@NotBlank(message = "BIC for FinancialInstitutionIdentification is mandatory")
	private String bic;

	public FinancialInstitutionIdentification()
	{
	}

	public FinancialInstitutionIdentification(String bic)
	{
		this.bic = bic;
	}

	public String getBic()
	{
		return bic;
	}

	public void setBic(String bic)
	{
		this.bic = bic;
	}
}
