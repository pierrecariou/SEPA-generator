package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentIdentification
{
	@XmlElement(name = "InstrId", required = true)
	@Size(min = 1, max = 35)
	private String instructionIdentification;

	@XmlElement(name = "EndToEndId")
	@Size(min = 1, max = 35)
	@NotBlank(message = "EndToEndIdentification from PaymentIdentification must not be blank")
	@CsvBindByName(column = "end_to_end_id")
	private String endToEndIdentification;

	public PaymentIdentification()
	{
	}

	public PaymentIdentification(String endToEndIdentification)
	{
		this.endToEndIdentification = endToEndIdentification;
	}

	public String getInstructionIdentification()
	{
		return instructionIdentification;
	}

	public void setInstructionIdentification(String instructionIdentification)
	{
		this.instructionIdentification = instructionIdentification;
	}

	public String getEndToEndIdentification()
	{
		return endToEndIdentification;
	}

	public void setEndToEndIdentification(String endToEndIdentification)
	{
		this.endToEndIdentification = endToEndIdentification;
	}
}
