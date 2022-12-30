package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

import com.opencsv.bean.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class RemittanceInformation
{
	@XmlElement(name = "Ustrd")
	@Size(min = 1, max = 140)
	@NotBlank(message = "Unstructured from RemittanceInformation is mandatory")
	@CsvBindByName(column = "information")
	private String unstructured;

	public RemittanceInformation()
	{
	}

	public RemittanceInformation(String unstructured)
	{
		this.unstructured = unstructured;
	}

	public String getUnstructured()
	{
		return unstructured;
	}

	public void setUnstructured(String unstructured)
	{
		this.unstructured = unstructured;
	}
}
