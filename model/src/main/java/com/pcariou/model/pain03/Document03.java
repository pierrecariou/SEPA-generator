package com.pcariou.model.pain03;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ISO 20022 pain.001.001.03 - SEPA Credit Transfer (output-only DTOs).
 *
 * <p>Root element {@code <Document>} with child {@code <CstmrCdtTrfInitn>}.
 * All message parts are kept as static nested classes: they are pure,
 * write-only data carriers used by {@code Pain03Writer} and have no validation
 * or CSV concerns. Field declaration order matches the schema sequence order of
 * pain.001.001.03 ({@code CustomerCreditTransferInitiationV03}).
 *
 * <p>Differences from the .09 DTOs, taken from the authentic schema:
 * <ul>
 *   <li>{@code PaymentInstructionInformation3} carries the optional
 *       {@code NbOfTxs}/{@code CtrlSum} of the payment block and the
 *       {@code ChrgBr} that .09 no longer has at that level</li>
 *   <li>{@code ReqdExctnDt} is a plain {@code ISODate}, not a
 *       {@code Dt}/{@code DtTm} choice</li>
 *   <li>agents are identified by {@code BIC}, not {@code BICFI}</li>
 * </ul>
 *
 * <p>Fields are private (like the .02 and .09 models): the JAXB runtime
 * accesses them reflectively, which avoids the bytecode-injection path that
 * fails for public fields on modern JDKs.
 */
@XmlRootElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")
@XmlAccessorType(XmlAccessType.FIELD)
public class Document03
{
	@XmlElement(name = "CstmrCdtTrfInitn")
	private CustomerCreditTransferInitiation customerCreditTransferInitiation;

	public Document03()
	{
	}

	public Document03(CustomerCreditTransferInitiation customerCreditTransferInitiation)
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

		public CustomerCreditTransferInitiation(GroupHeader groupHeader,
				List<PaymentInformation> paymentInformation)
		{
			this.groupHeader = groupHeader;
			this.paymentInformation = paymentInformation;
		}
	}

	// ── <GrpHdr> — no <Grpg> in .03 ──────────────────────────────────────────

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
	 * Structured postal address ({@code <PstlAdr>}, {@code PostalAddress6});
	 * element order follows the pain.001.001.03 schema sequence. The schema also
	 * allows free-text {@code AdrLine} elements, which the shared model never
	 * produces, so only the structured elements are emitted.
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

	/** .03 carries proprietary ids (e.g. SIRET) as {@code OrgId/Othr/Id}. */
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

		@XmlElement(name = "NbOfTxs")
		private String numberOfTransactions;

		@XmlElement(name = "CtrlSum")
		private String controlSum;

		@XmlElement(name = "PmtTpInf")
		private PaymentTypeInformation paymentTypeInformation;

		@XmlElement(name = "ReqdExctnDt")
		private String requestedExecutionDate;

		@XmlElement(name = "Dbtr")
		private Party debtor;

		@XmlElement(name = "DbtrAcct")
		private Account debtorAccount;

		@XmlElement(name = "DbtrAgt")
		private Agent debtorAgent;

		@XmlElement(name = "ChrgBr")
		private final String chargeBearer = "SLEV";

		@XmlElement(name = "CdtTrfTxInf")
		private List<CreditTransferTransaction> creditTransferTransactionInformation =
				new ArrayList<CreditTransferTransaction>();

		public PaymentInformation()
		{
		}

		public PaymentInformation(String paymentInformationIdentification,
				String numberOfTransactions, String controlSum,
				PaymentTypeInformation paymentTypeInformation, String requestedExecutionDate,
				Party debtor, Account debtorAccount, Agent debtorAgent,
				List<CreditTransferTransaction> creditTransferTransactionInformation)
		{
			this.paymentInformationIdentification = paymentInformationIdentification;
			this.numberOfTransactions = numberOfTransactions;
			this.controlSum = controlSum;
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

		public Agent(String bic)
		{
			this.financialInstitutionIdentification = new FinancialInstitutionIdentification(bic);
		}
	}

	/** .03 uses {@code BIC}, like .02; {@code BICFI} only appears from .09. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class FinancialInstitutionIdentification
	{
		@XmlElement(name = "BIC")
		private String bic;

		public FinancialInstitutionIdentification()
		{
		}

		public FinancialInstitutionIdentification(String bic)
		{
			this.bic = bic;
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
