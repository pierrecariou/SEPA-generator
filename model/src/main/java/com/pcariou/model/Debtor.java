package com.pcariou.model;

import javax.xml.bind.annotation.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Debtor
{
	@XmlElement(name = "Nm")
	private String name;

	public Debtor()
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
}
