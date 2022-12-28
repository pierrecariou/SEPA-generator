package com.pcariou.model;

import javax.xml.bind.annotation.*;

/**
 * ISO 20022 pain.001.001.02 - Swift MT101
 *
 */
@XmlType(name = "InitgPty")
@XmlAccessorType(XmlAccessType.FIELD)
public class InitiatingParty
{
	@XmlElement(name = "Nm")
	private String name;

	@XmlElement(name = "Id")
	private PartyIdentification partyIdentification;

	public InitiatingParty() {
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
