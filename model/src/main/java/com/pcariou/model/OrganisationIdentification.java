package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlType(name = "OrgId")
@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisationIdentification
{
	@XmlElement(name = "PrtryId")
	@NotNull(message = "ProprietaryIdentification for OrganisationIdentification is mandatory")
	private ProprietaryIdentification proprietaryIdentification;

	public OrganisationIdentification() {
	}

	public OrganisationIdentification(ProprietaryIdentification proprietaryIdentification) {
		this.proprietaryIdentification = proprietaryIdentification;
	}

	public ProprietaryIdentification getProprietaryIdentification() {
		return proprietaryIdentification;
	}

	public void setProprietaryIdentification(ProprietaryIdentification proprietaryIdentification) {
		this.proprietaryIdentification = proprietaryIdentification;
	}
}
