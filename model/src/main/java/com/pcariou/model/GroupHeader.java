package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class GroupHeader
{
	@XmlElement(name = "MsgId")
	@NotBlank(message = "Message identification is mandatory")
	private String messageIdentification;

	@XmlElement(name = "CreDtTm")
	@NotBlank(message = "Creation date time is mandatory")
	private String creationDateTime;

	@XmlElement(name = "BtchBookg")
	private Boolean batchBooking;

	@XmlElement(name = "NbOfTxs")
	@NotBlank(message = "Number of transactions is mandatory")
	private String numberOfTransactions;

	@XmlElement(name = "CtrlSum")
	private String controlSum;

	@XmlElement(name = "Grpg")
	@NotBlank(message = "Grouping is mandatory")
	private final String grouping = "MIXD";

	@XmlElement(name = "InitgPty")
	@NotNull(message = "Initiating party is mandatory")
	private InitiatingParty initiatingParty;

	public GroupHeader() {
	}

	public GroupHeader(String messageIdentification, String creationDateTime, String numberOfTransactions, InitiatingParty initiatingParty) {
		this.messageIdentification = messageIdentification;
		this.creationDateTime = creationDateTime;
		this.numberOfTransactions = numberOfTransactions;
		this.initiatingParty = initiatingParty;
	}

	public String getMessageIdentification() {
		return messageIdentification;
	}

	public void setMessageIdentification(String messageIdentification) {
		this.messageIdentification = messageIdentification;
	}

	public String getCreationDateTime() {
		return creationDateTime;
	}

	public void setCreationDateTime(String creationDateTime) {
		this.creationDateTime = creationDateTime;
	}

	public Boolean getBatchBooking() {
		return batchBooking;
	}

	public void setBatchBooking(Boolean batchBooking) {
		this.batchBooking = batchBooking;
	}

	public String getNumberOfTransactions() {
		return numberOfTransactions;
	}

	public void setNumberOfTransactions(String numberOfTransactions) {
		this.numberOfTransactions = numberOfTransactions;
	}

	public String getControlSum() {
		return controlSum;
	}

	public void setControlSum(String controlSum) {
		this.controlSum = controlSum;
	}

	public String getGrouping() {
		return grouping;
	}

	public InitiatingParty getInitiatingParty() {
		return initiatingParty;
	}

	public void setInitiatingParty(InitiatingParty initiatingParty) {
		this.initiatingParty = initiatingParty;
	}
}
