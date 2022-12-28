package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlType(name = "PrtryId")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProprietaryIdentification
{
	@XmlElement(name = "Id")
	@NotBlank(message = "Id for ProprietaryIdentification is mandatory")
	private String id;

	public ProprietaryIdentification() {
	}

	public ProprietaryIdentification(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
