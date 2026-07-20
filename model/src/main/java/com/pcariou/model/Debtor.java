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
	@Size(max = 70, message = "Name for Debtor must be at most 70 characters")
	private String name;

	@XmlTransient
	private PostalAddress postalAddress;

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

	/**
	 * Optional postal address (pain.001.001.09 only — excluded from the
	 * pain.001.001.02 message, hence {@code @XmlTransient}). Validated
	 * upstream where the settings file is read.
	 */
	public PostalAddress getPostalAddress()
	{
		return postalAddress;
	}

	public void setPostalAddress(PostalAddress postalAddress)
	{
		this.postalAddress = postalAddress;
	}
}
