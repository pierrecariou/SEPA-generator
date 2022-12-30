package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.opencsv.bean.*;


/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Amount
{
	@XmlElement(name = "InstdAmt", required = true)
	@NotNull(message = "InstructedAmount from Amount is mandatory")
	@Valid
	@CsvRecurse()
	private InstructedAmount instructedAmount;

	public Amount() {
	}

	public Amount(InstructedAmount instructedAmount) {
		this.instructedAmount = instructedAmount;
	}

	public InstructedAmount getInstructedAmount() {
		return instructedAmount;
	}

	public void setInstructedAmount(InstructedAmount instructedAmount) {
		this.instructedAmount = instructedAmount;
	}
}
