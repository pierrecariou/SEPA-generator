package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class InstructedAmount
{
	@XmlValue
	@NotBlank(message = "The transaction amount is mandatory. Please provide an amount in the amount column (e.g. 1250.50)")
	@ValidAmount
	@CsvBindByName(column = "amount")
	private String instructedAmount;

	@XmlAttribute(name = "Ccy")
	private final String currency = "EUR";

	public InstructedAmount() {
	}

	public InstructedAmount(String instructedAmount) {
		this.instructedAmount = instructedAmount;
	}

	public String getInstructedAmount() {
		return instructedAmount;
	}

	public void setInstructedAmount(String instructedAmount) {
		this.instructedAmount = instructedAmount.replace(",", ".");
	}
}
