package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Debtor
{
	@XmlElement(name = "Nm")
	@NotBlank(message = "Name for Debtor is mandatory")
	private String name;

	public Debtor()
	{
	}

	public Debtor(String name)
	{
		this.name = name;
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
