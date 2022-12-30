package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlType(name = "InitgPty")
@XmlAccessorType(XmlAccessType.FIELD)
public class InitiatingParty
{
	@XmlElement(name = "Nm")
	@NotBlank(message = "Name for InitiatingParty is mandatory")
	private String name;

	@XmlElement(name = "Id")
	@NotNull(message = "Identification for InitiatingParty is mandatory")
	private PartyIdentification partyIdentification;

	public InitiatingParty() {
	}

	public InitiatingParty(String name, PartyIdentification partyIdentification) {
		this.name = name;
		this.partyIdentification = partyIdentification;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PartyIdentification getPartyIdentification() {
		return partyIdentification;
	}

	public void setPartyIdentification(PartyIdentification partyIdentification) {
		this.partyIdentification = partyIdentification;
	}
}
