package com.pcariou.service;

import org.json.simple.parser.*;
import org.json.simple.*;

import java.io.*;

import javax.validation.constraints.*;


public class DebtorInformations {
	@NotBlank(message = "The debtor's name is mandatory")
	public String name;

	@NotBlank(message = "The iban for the debtor is mandatory")
	@Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}$", message = "IBAN for debtor is not valid")
	public String iban;

	@NotBlank(message = "The bic for the debtor is mandatory")
	public String bic;

	public String initiatingPartyName;

	public String initiatingPartySiret;
	
	@NotBlank(message = "The execution date is mandatory")
	@Pattern(regexp = "^(\\d{4})-(\\d{2})-(\\d{2})$", message = "Execution date is not valid")
	public String requestedExecutionDate;

	public DebtorInformations() {
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(new FileReader("DebtorInformations.json"));

			JSONObject jsonObject = (JSONObject) obj;

			JSONObject debtor = (JSONObject) jsonObject.get("debtor");
			this.name = (String)debtor.get("name");
			this.iban = (String) debtor.get("iban");
			this.bic = (String) debtor.get("bic");

			JSONObject initiatingParty = (JSONObject) jsonObject.get("initiatingParty");
			this.initiatingPartyName = (String) initiatingParty.get("name");
			this.initiatingPartySiret = (String) initiatingParty.get("siret");

			this.requestedExecutionDate = (String) jsonObject.get("requestedExecutionDate");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
