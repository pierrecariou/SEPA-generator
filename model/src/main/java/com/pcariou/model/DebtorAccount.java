package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DebtorAccount
{
	@XmlElement(name = "Id", required = true)
	@NotNull(message = "AccountIdentification for DebtorAccount is mandatory")
	private AccountIdentification accountIdentification;

	@XmlElement(name = "Ccy")
	private String currency;

	public DebtorAccount()
	{
	}

	public DebtorAccount(AccountIdentification accountIdentification)
	{
		this.accountIdentification = accountIdentification;
		//this.currency = currency;
	}

	public AccountIdentification getAccountIdentification()
	{
		return accountIdentification;
	}

	public void setAccountIdentification(AccountIdentification accountIdentification)
	{
		this.accountIdentification = accountIdentification;
	}

	public String getCurrency()
	{
		return currency;
	}

	public void setCurrency(String currency)
	{
		this.currency = currency;
	}
}
