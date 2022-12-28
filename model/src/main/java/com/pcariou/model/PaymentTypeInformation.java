package com.pcariou.model;

import javax.xml.bind.annotation.*;

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
	private ServiceLevel serviceLevel;

	@XmlElement(name = "CtgyPurp")
	private String categoryPurpose;

	public PaymentTypeInformation()
	{
	}

	public String getInstructionPriority()
	{
		return instructionPriority;
	}

	public void setInstructionPriority(String instructionPriority)
	{
		this.instructionPriority = instructionPriority;
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
