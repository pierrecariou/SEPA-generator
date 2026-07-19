package com.pcariou.service;

import org.json.simple.parser.*;
import org.json.simple.*;

import java.time.LocalDate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import javax.validation.constraints.*;

import com.pcariou.model.PostalAddress;
import com.pcariou.model.ValidBic;
import com.pcariou.model.ValidIban;
import com.pcariou.model.ValidPostalAddress;


public class DebtorInformations {
	@NotBlank(message = "The debtor's name is mandatory")
	@Size(max = 70, message = "The debtor's name must be at most 70 characters")
	public String name;

	@NotBlank(message = "The IBAN for the debtor is mandatory")
	@ValidIban
	public String iban;

	@NotBlank(message = "The BIC for the debtor is mandatory")
	@ValidBic
	public String bic;

	@NotBlank(message = "The initiating party name is mandatory")
	@Size(max = 70, message = "The initiating party name must be at most 70 characters")
	public String initiatingPartyName;

	@NotBlank(message = "The initiating party SIRET is mandatory")
	@Pattern(regexp = "^[0-9]{14}$", message = "SIRET for initiating party is not valid (expected: 14 digits)")
	public String initiatingPartySiret;
	
	@NotBlank(message = "The execution date is mandatory")
	@Pattern(regexp = "^(\\d{4})-(\\d{2})-(\\d{2})$", message = "Execution date is not valid")
	public String requestedExecutionDate;

	/** Optional debtor postal address (pain.001.001.09 only); null when not configured. */
	@ValidPostalAddress(label = "debtor")
	public PostalAddress address;

	private static final File CONFIG_FILE =
			new File(System.getProperty("user.home"), ".sepa-generator-config.json");

	/**
	 * Resolves the config file location. Defaults to the user-home config file,
	 * but can be overridden via the {@code sepa.config.file} system property
	 * (used by tests to avoid depending on user-local paths). Production
	 * behaviour is unchanged when the property is not set.
	 */
	private static File resolveConfigFile() {
		String override = System.getProperty("sepa.config.file");
		if (override != null && !override.isEmpty()) {
			return new File(override);
		}
		return CONFIG_FILE;
	}

	public DebtorInformations(LocalDate requestedExecutionDate) throws IOException, ParseException, FileNotFoundException, IllegalArgumentException {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject;
		// UTF-8 to match ConfigStore's explicit encoding, and try-with-resources
		// so the config file handle is always released deterministically (a
		// lingering handle can block ConfigStore's atomic replace on Windows).
		try (Reader reader = new InputStreamReader(
				new FileInputStream(resolveConfigFile()), StandardCharsets.UTF_8)) {
			jsonObject = (JSONObject) parser.parse(reader);
		}

		JSONObject debtor = (JSONObject) jsonObject.get("debtor");
		this.name = (String) debtor.get("name");
		this.iban = com.pcariou.model.SepaFieldNormalizer.iban((String) debtor.get("iban"));
		this.bic = com.pcariou.model.SepaFieldNormalizer.bic((String) debtor.get("bic"));
		this.address = readAddress((JSONObject) debtor.get("address"));

		JSONObject initiatingParty = (JSONObject) jsonObject.get("initiatingParty");
		this.initiatingPartyName = (String) initiatingParty.get("name");
		this.initiatingPartySiret = (String) initiatingParty.get("siret");
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		this.requestedExecutionDate = requestedExecutionDate.format(formatter);
		if (requestedExecutionDate.isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("Execution date must be in the future");
		}
	}

	/** Reads the optional debtor address; returns null when absent or fully empty. */
	private static PostalAddress readAddress(JSONObject json) {
		if (json == null) {
			return null;
		}
		PostalAddress address = new PostalAddress(
				(String) json.get("street"),
				(String) json.get("buildingNumber"),
				(String) json.get("postcode"),
				(String) json.get("town"),
				(String) json.get("country"));
		return address.isEmpty() ? null : address;
	}
}
