package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountIdentification
{
	@XmlElement(name = "IBAN")
	@NotBlank(message = "IBAN for AccountIdentification is mandatory")
	private String iban;

	public AccountIdentification()
	{
	}

	public AccountIdentification(String iban)
	{
		this.iban = iban;
	}

	public String getIban()
	{
		return iban;
	}

	public void setIban(String iban)
	{
		this.iban = iban;
	}
}
