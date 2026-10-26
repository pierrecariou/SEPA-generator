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

	@XmlElement(name = "Issr")
	private String issuer;

	/**
	 * Scheme fields are not part of pain.001.001.02 ({@code GenericIdentification3}
	 * has no scheme name); they are carried for the .03/.09 writers only.
	 */
	@XmlTransient
	private String schemeCode;

	@XmlTransient
	private String schemeProprietary;

	public ProprietaryIdentification() {
	}

	public ProprietaryIdentification(String id) {
		this.id = id;
	}

	public static ProprietaryIdentification of(InitiatingPartyIdentifier identifier) {
		ProprietaryIdentification result = new ProprietaryIdentification(identifier.getId());
		result.issuer = identifier.getIssuer();
		result.schemeCode = identifier.getSchemeCode();
		result.schemeProprietary = identifier.getSchemeProprietary();
		return result;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getSchemeCode() {
		return schemeCode;
	}

	public String getSchemeProprietary() {
		return schemeProprietary;
	}
}
