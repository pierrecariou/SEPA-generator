package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CreditorAccount
{
	@XmlElement(name = "Id")
	@NotNull(message = "AccountIdentification for CreditorAccount is mandatory")
	@CsvRecurse()
	private AccountIdentification accountIdentification;

	public CreditorAccount()
	{
	}

	public CreditorAccount(AccountIdentification accountIdentification)
	{
		this.accountIdentification = accountIdentification;
	}

	public AccountIdentification getAccountIdentification()
	{
		return accountIdentification;
	}

	public void setAccountIdentification(AccountIdentification accountIdentification)
	{
		this.accountIdentification = accountIdentification;
	}
}
