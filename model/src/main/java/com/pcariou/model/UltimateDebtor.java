package com.pcariou.model;

import javax.xml.bind.annotation.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlType(name = "UltmtDbtr")
@XmlAccessorType(XmlAccessType.FIELD)
public class UltimateDebtor
{
	@XmlElement(name = "Nm")
	private String name;

	@XmlElement(name = "Id")
	private PartyIdentification partyIdentification;

	public UltimateDebtor()
	{
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public PartyIdentification getPartyIdentification()
	{
		return partyIdentification;
	}

	public void setPartyIdentification(PartyIdentification partyIdentification)
	{
		this.partyIdentification = partyIdentification;
	}

}
