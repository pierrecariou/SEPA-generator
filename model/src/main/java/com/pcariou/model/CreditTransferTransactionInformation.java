package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CreditTransferTransactionInformation
{
	@XmlElement(name = "PmtId")
	@NotNull(message = "PaymentIdentification for CreditTransferTransactionInformation is mandatory")
	private PaymentIdentification paymentIdentification;

	@XmlElement(name = "Amt")
	@NotNull(message = "Amount for CreditTransferTransactionInformation is mandatory")
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
	private UltimateCreditor ultimateCreditor;

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

	public PaymentTypeInformation getPaymentTypeInformation()
	{
		return paymentTypeInformation;
	}

	public void setPaymentTypeInformation(PaymentTypeInformation paymentTypeInformation)
	{
		this.paymentTypeInformation = paymentTypeInformation;
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

	public UltimateCreditor getUltimateCreditor()
	{
		return ultimateCreditor;
	}

	public void setUltimateCreditor(UltimateCreditor ultimateCreditor)
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
}
