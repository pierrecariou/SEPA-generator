package com.pcariou.model;

import javax.xml.bind.annotation.*;
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
	@CsvRecurse()
	private PaymentIdentification paymentIdentification;

	@XmlElement(name = "Amt")
	@NotNull(message = "Amount for CreditTransferTransactionInformation is mandatory")
	@CsvRecurse()
	private Amount amount;

	@XmlElement(name = "UltmtDbtr")
	private UltimateDebtor ultimateDebtor;

	@XmlElement(name = "CdtrAgt")
	private FinancialInstitutionIdentification creditorAgent;

	@XmlElement(name = "Cdtr")
	private PartyIdentification creditor;

	@XmlElement(name = "CdtrAcct")
	private AccountIdentification creditorAccount;

	@XmlElement(name = "UltmtCdtr")
	private PartyIdentification ultimateCreditor;

	@XmlElement(name = "Purp")
	private Purpose purpose;

	@XmlElement(name = "RmtInf")
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

	public FinancialInstitutionIdentification getCreditorAgent()
	{
		return creditorAgent;
	}

	public void setCreditorAgent(FinancialInstitutionIdentification creditorAgent)
	{
		this.creditorAgent = creditorAgent;
	}

	public PartyIdentification getCreditor()
	{
		return creditor;
	}

	public void setCreditor(PartyIdentification creditor)
	{
		this.creditor = creditor;
	}

	public AccountIdentification getCreditorAccount()
	{
		return creditorAccount;
	}

	public void setCreditorAccount(AccountIdentification creditorAccount)
	{
		this.creditorAccount = creditorAccount;
	}

	public RemittanceInformation getRemittanceInformation()
	{
		return remittanceInformation;
	}

	public void setRemittanceInformation(RemittanceInformation remittanceInformation)
	{
		this.remittanceInformation = remittanceInformation;
	}

	public UltimateDebtor getUltimateDebtor()
	{
		return ultimateDebtor;
	}

	public void setUltimateDebtor(UltimateDebtor ultimateDebtor)
	{
		this.ultimateDebtor = ultimateDebtor;
	}

	public Purpose getPurpose()
	{
		return purpose;
	}

	public void setPurpose(Purpose purpose)
	{
		this.purpose = purpose;
	}

	public PartyIdentification getUltimateCreditor()
	{
		return ultimateCreditor;
	}

	public void setUltimateCreditor(PartyIdentification ultimateCreditor)
	{
		this.ultimateCreditor = ultimateCreditor;
	}
}
