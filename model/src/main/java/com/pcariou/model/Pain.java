package com.pcariou.model;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import javax.validation.constraints.*;

//Pain.001 is a payments initiation message by ISO 20022. It depicts a Credit Transfer message in XML format.

/**
 * ISO 20022 pain.001.001.02 - SEPA Credit Transfer
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Pain
{
	@XmlElement(name = "GrpHdr")
	@NotNull(message = "GroupHeader is mandatory")
	private GroupHeader groupHeader;

	@XmlElement(name = "PmtInf")
	@NotNull(message = "PaymentInformation is mandatory")
	private ArrayList<PaymentInformation> paymentInformation;

	public Pain() {
	}

	public Pain(GroupHeader groupHeader, ArrayList<PaymentInformation> paymentInformation) {
		this.groupHeader = groupHeader;
		this.paymentInformation = paymentInformation;
	}

	public GroupHeader getGroupHeader() {
		return groupHeader;
	}

	public void setGroupHeader(GroupHeader groupHeader) {
		this.groupHeader = groupHeader;
	}

	public ArrayList<PaymentInformation> getPaymentInformation() {
		return paymentInformation;
	}

	public void setPaymentInformation(ArrayList<PaymentInformation> paymentInformation) {
		this.paymentInformation = paymentInformation;
	}
}
