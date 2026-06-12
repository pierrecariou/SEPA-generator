package com.pcariou.model.pain09;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ISO 20022 pain.001.001.09 - SEPA Credit Transfer (output-only DTOs).
 *
 * <p>Root element {@code <Document>} with child {@code <CstmrCdtTrfInitn>}.
 * All message parts are kept as static nested classes: they are pure,
 * write-only data carriers used by {@code Pain09Writer} and have no
 * validation or CSV concerns. Field declaration order matches the schema
 * sequence order of pain.001.001.09.
 *
 * <p>Fields are private (like the .02 model): the JAXB runtime accesses them
 * reflectively, which avoids the bytecode-injection path that fails for
 * public fields on modern JDKs.
 */
@XmlRootElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.09")
@XmlAccessorType(XmlAccessType.FIELD)
public class Document09
{
	@XmlElement(name = "CstmrCdtTrfInitn")
	private CustomerCreditTransferInitiation customerCreditTransferInitiation;

	public Document09()
	{
	}

	public Document09(CustomerCreditTransferInitiation customerCreditTransferInitiation)
	{
		this.customerCreditTransferInitiation = customerCreditTransferInitiation;
	}

	public CustomerCreditTransferInitiation getCustomerCreditTransferInitiation()
	{
		return customerCreditTransferInitiation;
	}

	// ── <CstmrCdtTrfInitn> ───────────────────────────────────────────────────

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class CustomerCreditTransferInitiation
	{
		@XmlElement(name = "GrpHdr")
		private GroupHeader groupHeader;

		@XmlElement(name = "PmtInf")
		private List<PaymentInformation> paymentInformation = new ArrayList<PaymentInformation>();

		public CustomerCreditTransferInitiation()
		{
		}

		public CustomerCreditTransferInitiation(GroupHeader groupHeader, List<PaymentInformation> paymentInformation)
		{
			this.groupHeader = groupHeader;
			this.paymentInformation = paymentInformation;
		}
	}

	// ── <GrpHdr> — no <Grpg> in .09 ──────────────────────────────────────────

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class GroupHeader
	{
		@XmlElement(name = "MsgId")
		private String messageIdentification;

		@XmlElement(name = "CreDtTm")
		private String creationDateTime;

		@XmlElement(name = "NbOfTxs")
		private String numberOfTransactions;

		@XmlElement(name = "CtrlSum")
		private String controlSum;

		@XmlElement(name = "InitgPty")
		private Party initiatingParty;

		public GroupHeader()
		{
		}

		public GroupHeader(String messageIdentification, String creationDateTime,
				String numberOfTransactions, String controlSum, Party initiatingParty)
		{
			this.messageIdentification = messageIdentification;
			this.creationDateTime = creationDateTime;
			this.numberOfTransactions = numberOfTransactions;
			this.controlSum = controlSum;
			this.initiatingParty = initiatingParty;
		}
	}

	// ── Parties ──────────────────────────────────────────────────────────────

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Party
	{
		@XmlElement(name = "Nm")
		private String name;

		@XmlElement(name = "PstlAdr")
		private PostalAddress postalAddress;

		@XmlElement(name = "Id")
		private PartyIdentification identification;

		public Party()
		{
		}

		public Party(String name)
		{
			this.name = name;
		}

		public Party(String name, PostalAddress postalAddress)
		{
			this.name = name;
			this.postalAddress = postalAddress;
		}

		public Party(String name, PartyIdentification identification)
		{
			this.name = name;
			this.identification = identification;
		}
	}

	/**
	 * Structured postal address ({@code <PstlAdr>}); element order follows
	 * the pain.001.001.09 schema sequence.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PostalAddress
	{
		@XmlElement(name = "StrtNm")
		private String streetName;

		@XmlElement(name = "BldgNb")
		private String buildingNumber;

		@XmlElement(name = "PstCd")
		private String postCode;

		@XmlElement(name = "TwnNm")
		private String townName;

		@XmlElement(name = "Ctry")
		private String country;

		public PostalAddress()
		{
		}

		public PostalAddress(String streetName, String buildingNumber, String postCode,
				String townName, String country)
		{
			this.streetName = streetName;
			this.buildingNumber = buildingNumber;
			this.postCode = postCode;
			this.townName = townName;
			this.country = country;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PartyIdentification
	{
		@XmlElement(name = "OrgId")
		private OrganisationIdentification organisationIdentification;

		public PartyIdentification()
		{
		}

		public PartyIdentification(OrganisationIdentification organisationIdentification)
		{
			this.organisationIdentification = organisationIdentification;
		}
	}

	/** .09 carries proprietary ids (e.g. SIRET) as {@code OrgId/Othr/Id}. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class OrganisationIdentification
	{
		@XmlElement(name = "Othr")
		private GenericIdentification other;

		public OrganisationIdentification()
		{
		}

		public OrganisationIdentification(GenericIdentification other)
		{
			this.other = other;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class GenericIdentification
	{
		@XmlElement(name = "Id")
		private String id;

		public GenericIdentification()
		{
		}

		public GenericIdentification(String id)
		{
			this.id = id;
		}
	}

	// ── <PmtInf> ─────────────────────────────────────────────────────────────

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PaymentInformation
	{
		@XmlElement(name = "PmtInfId")
		private String paymentInformationIdentification;

		@XmlElement(name = "PmtMtd")
		private final String paymentMethod = "TRF";

		@XmlElement(name = "PmtTpInf")
		private PaymentTypeInformation paymentTypeInformation;

		@XmlElement(name = "ReqdExctnDt")
		private DateAndDateTimeChoice requestedExecutionDate;

		@XmlElement(name = "Dbtr")
		private Party debtor;

		@XmlElement(name = "DbtrAcct")
		private Account debtorAccount;

		@XmlElement(name = "DbtrAgt")
		private Agent debtorAgent;

		@XmlElement(name = "CdtTrfTxInf")
		private List<CreditTransferTransaction> creditTransferTransactionInformation =
				new ArrayList<CreditTransferTransaction>();

		public PaymentInformation()
		{
		}

		public PaymentInformation(String paymentInformationIdentification,
				PaymentTypeInformation paymentTypeInformation, DateAndDateTimeChoice requestedExecutionDate,
				Party debtor, Account debtorAccount, Agent debtorAgent,
				List<CreditTransferTransaction> creditTransferTransactionInformation)
		{
			this.paymentInformationIdentification = paymentInformationIdentification;
			this.paymentTypeInformation = paymentTypeInformation;
			this.requestedExecutionDate = requestedExecutionDate;
			this.debtor = debtor;
			this.debtorAccount = debtorAccount;
			this.debtorAgent = debtorAgent;
			this.creditTransferTransactionInformation = creditTransferTransactionInformation;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PaymentTypeInformation
	{
		@XmlElement(name = "SvcLvl")
		private final ServiceLevel serviceLevel = new ServiceLevel();
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ServiceLevel
	{
		@XmlElement(name = "Cd")
		private final String code = "SEPA";
	}

	/** In .09 the execution date is a {@code Dt}/{@code DtTm} choice; we emit {@code Dt}. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class DateAndDateTimeChoice
	{
		@XmlElement(name = "Dt")
		private String date;

		public DateAndDateTimeChoice()
		{
		}

		public DateAndDateTimeChoice(String date)
		{
			this.date = date;
		}
	}

	// ── Accounts and agents ──────────────────────────────────────────────────

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Account
	{
		@XmlElement(name = "Id")
		private AccountIdentification identification;

		public Account()
		{
		}

		public Account(String iban)
		{
			this.identification = new AccountIdentification(iban);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class AccountIdentification
	{
		@XmlElement(name = "IBAN")
		private String iban;

		public AccountIdentification()
		{
		}

		public AccountIdentification(String iban)
		{
			this.iban = iban;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Agent
	{
		@XmlElement(name = "FinInstnId")
		private FinancialInstitutionIdentification financialInstitutionIdentification;

		public Agent()
		{
		}

		public Agent(String bicfi)
		{
			this.financialInstitutionIdentification = new FinancialInstitutionIdentification(bicfi);
		}
	}

	/** .09 uses {@code BICFI} where .02 used {@code BIC}. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class FinancialInstitutionIdentification
	{
		@XmlElement(name = "BICFI")
		private String bicfi;

		public FinancialInstitutionIdentification()
		{
		}

		public FinancialInstitutionIdentification(String bicfi)
		{
			this.bicfi = bicfi;
		}
	}

	// ── <CdtTrfTxInf> ────────────────────────────────────────────────────────

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class CreditTransferTransaction
	{
		@XmlElement(name = "PmtId")
		private PaymentIdentification paymentIdentification;

		@XmlElement(name = "Amt")
		private AmountType amount;

		@XmlElement(name = "CdtrAgt")
		private Agent creditorAgent;

		@XmlElement(name = "Cdtr")
		private Party creditor;

		@XmlElement(name = "CdtrAcct")
		private Account creditorAccount;

		@XmlElement(name = "RmtInf")
		private RemittanceInformation remittanceInformation;

		public CreditTransferTransaction()
		{
		}

		public CreditTransferTransaction(PaymentIdentification paymentIdentification, AmountType amount,
				Agent creditorAgent, Party creditor, Account creditorAccount,
				RemittanceInformation remittanceInformation)
		{
			this.paymentIdentification = paymentIdentification;
			this.amount = amount;
			this.creditorAgent = creditorAgent;
			this.creditor = creditor;
			this.creditorAccount = creditorAccount;
			this.remittanceInformation = remittanceInformation;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PaymentIdentification
	{
		@XmlElement(name = "EndToEndId")
		private String endToEndIdentification;

		public PaymentIdentification()
		{
		}

		public PaymentIdentification(String endToEndIdentification)
		{
			this.endToEndIdentification = endToEndIdentification;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class AmountType
	{
		@XmlElement(name = "InstdAmt")
		private InstructedAmount instructedAmount;

		public AmountType()
		{
		}

		public AmountType(InstructedAmount instructedAmount)
		{
			this.instructedAmount = instructedAmount;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class InstructedAmount
	{
		@XmlValue
		private String value;

		@XmlAttribute(name = "Ccy")
		private String currency;

		public InstructedAmount()
		{
		}

		public InstructedAmount(String value, String currency)
		{
			this.value = value;
			this.currency = currency;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class RemittanceInformation
	{
		@XmlElement(name = "Ustrd")
		private String unstructured;

		public RemittanceInformation()
		{
		}

		public RemittanceInformation(String unstructured)
		{
			this.unstructured = unstructured;
		}
	}
}
