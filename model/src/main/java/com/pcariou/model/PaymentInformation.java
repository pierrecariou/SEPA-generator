package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;
import java.util.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentInformation
{
	@XmlElement(name = "PmtInfId")
	private String paymentInformationIdentification;

	@XmlElement(name = "PmtMtd")
	@NotBlank(message = "PaymentMethod for PaymentInformation is mandatory")
	private final String paymentMethod = "TRF";

	@XmlElement(name = "PmtTpInf")
	private PaymentTypeInformation paymentTypeInformation;

	@XmlElement(name = "ReqdExctnDt")
	@NotBlank(message = "RequestedExecutionDate for PaymentInformation is mandatory")
	private String requestedExecutionDate;

	@XmlElement(name = "Dbtr")
	@NotNull(message = "Debtor for PaymentInformation is mandatory")
	private Debtor debtor;

	@XmlElement(name = "DbtrAcct")
	@NotNull(message = "DebtorAccount for PaymentInformation is mandatory")
	private DebtorAccount debtorAccount;

	@XmlElement(name = "DbtrAgt")
	@NotNull(message = "DebtorAgent for PaymentInformation is mandatory")
	private DebtorAgent debtorAgent;

	@XmlElement(name = "UlmtDbtr")
	private UltimateDebtor ultimateDebtor;

	@XmlElement(name = "ChrgBr")
	private String chargeBearer;

	@XmlElement(name = "CdtTrfTxInf")
	@NotNull(message = "CreditTransferTransactionInformation for PaymentInformation is mandatory")
	private ArrayList<CreditTransferTransactionInformation> creditTransferTransactionInformation;

	public PaymentInformation()
	{
	}

	public PaymentInformation(String requestedExecutionDate, Debtor debtor, DebtorAccount debtorAccount, DebtorAgent debtorAgent, ArrayList<CreditTransferTransactionInformation> creditTransferTransactionInformation)
	{
		this.requestedExecutionDate = requestedExecutionDate;
		this.debtor = debtor;
		this.debtorAccount = debtorAccount;
		this.debtorAgent = debtorAgent;
		this.creditTransferTransactionInformation = creditTransferTransactionInformation;
	}

	public String getPaymentInformationIdentification()
	{
		return paymentInformationIdentification;
	}

	public void setPaymentInformationIdentification(String paymentInformationIdentification)
	{
		this.paymentInformationIdentification = paymentInformationIdentification;
	}

	public String getPaymentMethod()
	{
		return paymentMethod;
	}

	public PaymentTypeInformation getPaymentTypeInformation()
	{
		return paymentTypeInformation;
	}

	public void setPaymentTypeInformation(PaymentTypeInformation paymentTypeInformation)
	{
		this.paymentTypeInformation = paymentTypeInformation;
	}

	public String getRequestedExecutionDate()
	{
		return requestedExecutionDate;
	}

	public void setRequestedExecutionDate(String requestedExecutionDate)
	{
		this.requestedExecutionDate = requestedExecutionDate;
	}

	public Debtor getDebtor()
	{
		return debtor;
	}

	public void setDebtor(Debtor debtor)
	{
		this.debtor = debtor;
	}

	public DebtorAccount getDebtorAccount()
	{
		return debtorAccount;
	}

	public void setDebtorAccount(DebtorAccount debtorAccount)
	{
		this.debtorAccount = debtorAccount;
	}

	public DebtorAgent getDebtorAgent()
	{
		return debtorAgent;
	}

	public void setDebtorAgent(DebtorAgent debtorAgent)
	{
		this.debtorAgent = debtorAgent;
	}

	public UltimateDebtor getUltimateDebtor()
	{
		return ultimateDebtor;
	}

	public void setUltimateDebtor(UltimateDebtor ultimateDebtor)
	{
		this.ultimateDebtor = ultimateDebtor;
	}

	public String getChargeBearer()
	{
		return chargeBearer;
	}

	public void setChargeBearer()
	{
		this.chargeBearer = "SLEV";
	}
}
