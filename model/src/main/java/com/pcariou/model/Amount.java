package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.*;


/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Amount
{
	//declare xml element InstdAmt with Ccy="EUR" inside
	@XmlElement(name = "InstdAmt", required = true)
	//@XmlAttribute(name = "Ccy", required = true)
	@NotBlank(message = "InstructedAmount from Amount is mandatory")
	@CsvBindByName(column = "amount")
	private String instructedAmount;

	public Amount()
	{
	}

	public Amount(String instructedAmount)
	{
		this.instructedAmount = instructedAmount;
	}

	public String getInstructedAmount()
	{
		return instructedAmount;
	}

	public void setInstructedAmount(String instructedAmount)
	{
		this.instructedAmount = instructedAmount;
	}
}
