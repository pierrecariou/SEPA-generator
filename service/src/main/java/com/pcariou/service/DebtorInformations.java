package com.pcariou.service;

import org.json.simple.parser.*;
import org.json.simple.*;

import java.io.*;

public class DebtorInformations {
	public String name;
	public String iban;
	public String bic;

	public String initiatingPartyName;
	public String initiatingPartySiret;
	
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
