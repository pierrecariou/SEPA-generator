package com.pcariou.model;

import javax.xml.bind.annotation.*;
import javax.validation.constraints.*;

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PartyIdentification
{
	@XmlElement(name = "OrgId")
	@NotNull(message = "OrganisationIdentification for PartyIdentification is mandatory")
	private OrganisationIdentification organisationIdentification;

	public PartyIdentification() {
	}

	public PartyIdentification(OrganisationIdentification organisationIdentification) {
		this.organisationIdentification = organisationIdentification;
	}

	public OrganisationIdentification getOrganisationIdentification() {
		return organisationIdentification;
	}

	public void setOrganisationIdentification(OrganisationIdentification organisationIdentification) {
		this.organisationIdentification = organisationIdentification;
	}
}
