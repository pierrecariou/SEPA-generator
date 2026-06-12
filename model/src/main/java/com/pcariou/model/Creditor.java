package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Creditor
{
	@XmlElement(name = "Nm")
	@NotBlank(message = "Name for Creditor is mandatory")
	@CsvBindByName()
	private String name;

	@XmlTransient
	@ValidPostalAddress(label = "creditor")
	@CsvRecurse()
	private PostalAddress postalAddress;

	@XmlElement(name = "Id")
	private PartyIdentification creditorIdentification;

	public Creditor()
	{
	}

	public Creditor(String name)
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

	public PartyIdentification getCreditorIdentification()
	{
		return creditorIdentification;
	}

	public void setCreditorIdentification(PartyIdentification creditorIdentification)
	{
		this.creditorIdentification = creditorIdentification;
	}

	/**
	 * Optional postal address (pain.001.001.09 only — excluded from the
	 * pain.001.001.02 message, hence {@code @XmlTransient}).
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
