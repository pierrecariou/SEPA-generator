package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentTypeInformation
{
	@XmlElement(name = "InstrPrty")
	private String instructionPriority;

	@XmlElement(name = "SvcLvl")
	@NotNull(message = "ServiceLevel for PaymentTypeInformation is mandatory")
	private ServiceLevel serviceLevel;

	@XmlElement(name = "CtgyPurp")
	private String categoryPurpose;

	public PaymentTypeInformation()
	{
	}

	public PaymentTypeInformation(ServiceLevel serviceLevel)
	{
		this.serviceLevel = serviceLevel;
	}

	public String getInstructionPriority()
	{
		return instructionPriority;
	}

	public void setInstructionPriority(String instructionPriority, ServiceLevel serviceLevel)
	{
		this.instructionPriority = instructionPriority;
		this.serviceLevel = serviceLevel;
	}

	public ServiceLevel getServiceLevel()
	{
		return serviceLevel;
	}

	public void setServiceLevel(ServiceLevel serviceLevel)
	{
		this.serviceLevel = serviceLevel;
	}

	public String getCategoryPurpose()
	{
		return categoryPurpose;
	}

	public void setCategoryPurpose(String categoryPurpose)
	{
		this.categoryPurpose = categoryPurpose;
	}
}
