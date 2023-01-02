package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountIdentification
{
	@XmlElement(name = "IBAN")
	@NotBlank(message = "IBAN for AccountIdentification is mandatory")
	@Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}$", message = "IBAN for AccountIdentification is invalid")
	@CsvBindByName(column = "IBAN")
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
