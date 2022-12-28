package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlType(name = "SvcLvl")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceLevel
{
	@XmlElement(name = "Cd")
	@NotBlank(message = "Code for ServiceLevel is mandatory")
	private final String code = "SEPA";

	public ServiceLevel()
	{
	}

	public String getCode()
	{
		return code;
	}
}
