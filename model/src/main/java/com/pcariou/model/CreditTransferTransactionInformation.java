package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.opencsv.bean.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CreditTransferTransactionInformation
{
	@XmlElement(name = "PmtId")
	@NotNull(message = "PaymentIdentification for CreditTransferTransactionInformation is mandatory")
	@Valid()
	@CsvRecurse()
	private PaymentIdentification paymentIdentification;

	@XmlElement(name = "Amt")
	@NotNull(message = "Amount for CreditTransferTransactionInformation is mandatory")
	@Valid()
	@CsvRecurse()
	private Amount amount;

	@XmlElement(name = "UltmtDbtr")
	private UltimateDebtor ultimateDebtor;

	@XmlElement(name = "CdtrAgt")
	@NotNull(message = "CreditorAgent for CreditTransferTransactionInformation is mandatory")
	@Valid()
	@CsvRecurse()
	private CreditorAgent creditorAgent;

	@XmlElement(name = "Cdtr")
	@NotNull(message = "Creditor for CreditTransferTransactionInformation is mandatory")
	@Valid()
	@CsvRecurse()
	private Creditor creditor;

	@XmlElement(name = "CdtrAcct")
	@NotNull(message = "CreditorAccount for CreditTransferTransactionInformation is mandatory")
	@Valid()
	@CsvRecurse()
	private CreditorAccount creditorAccount;

	@XmlElement(name = "UltmtCdtr")
	private PartyIdentification ultimateCreditor;

	@XmlElement(name = "Purp")
	private Purpose purpose;

	@XmlElement(name = "RmtInf")
	@CsvRecurse()
	private RemittanceInformation remittanceInformation;

	public CreditTransferTransactionInformation()
	{
	}

	public CreditTransferTransactionInformation(PaymentIdentification paymentIdentification, Amount amount)
	{
		this.paymentIdentification = paymentIdentification;
		this.amount = amount;
	}

	public PaymentIdentification getPaymentIdentification()
	{
		return paymentIdentification;
	}

	public void setPaymentIdentification(PaymentIdentification paymentIdentification)
	{
		this.paymentIdentification = paymentIdentification;
	}

	public Amount getAmount()
	{
		return amount;
	}

	public void setAmount(Amount amount)
	{
		this.amount = amount;
	}

	public UltimateDebtor getUltimateDebtor()
	{
		return ultimateDebtor;
	}

	public void setUltimateDebtor(UltimateDebtor ultimateDebtor)
	{
		this.ultimateDebtor = ultimateDebtor;
	}

	public CreditorAgent getCreditorAgent()
	{
		return creditorAgent;
	}

	public void setCreditorAgent(CreditorAgent creditorAgent)
	{
		this.creditorAgent = creditorAgent;
	}

	public Creditor getCreditor()
	{
		return creditor;
	}

	public void setCreditor(Creditor creditor)
	{
		this.creditor = creditor;
	}

	public CreditorAccount getCreditorAccount()
	{
		return creditorAccount;
	}

	public void setCreditorAccount(CreditorAccount creditorAccount)
	{
		this.creditorAccount = creditorAccount;
	}

	public PartyIdentification getUltimateCreditor()
	{
		return ultimateCreditor;
	}

	public void setUltimateCreditor(PartyIdentification ultimateCreditor)
	{
		this.ultimateCreditor = ultimateCreditor;
	}

	public Purpose getPurpose()
	{
		return purpose;
	}

	public void setPurpose(Purpose purpose)
	{
		this.purpose = purpose;
	}

	public RemittanceInformation getRemittanceInformation()
	{
		return remittanceInformation;
	}

	public void setRemittanceInformation(RemittanceInformation remittanceInformation)
	{
		this.remittanceInformation = remittanceInformation;
	}
}
