package com.pcariou.model;

import com.opencsv.bean.CsvBindByName;

/**
 * Optional postal address for a payment party (debtor or creditor).
 *
 * <p>All fields are optional, but when any field is provided the address must
 * contain at least a town/city and a 2-letter ISO country code — enforced by
 * {@link ValidPostalAddress}. The address is only emitted in pain.001.001.09
 * output; it is deliberately excluded from the pain.001.001.02 message, so
 * the fields referencing this class are marked {@code @XmlTransient} there.
 *
 * <p>Creditor addresses are read from the optional CSV columns
 * {@code street}, {@code building_number}, {@code postcode}, {@code town}
 * and {@code country}; the debtor address comes from the settings file.
 */
public class PostalAddress
{
	@CsvBindByName(column = "street")
	private String street;

	@CsvBindByName(column = "building_number")
	private String buildingNumber;

	@CsvBindByName(column = "postcode")
	private String postcode;

	@CsvBindByName(column = "town")
	private String town;

	@CsvBindByName(column = "country")
	private String country;

	public PostalAddress()
	{
	}

	public PostalAddress(String street, String buildingNumber, String postcode, String town, String country)
	{
		this.street = street;
		this.buildingNumber = buildingNumber;
		this.postcode = postcode;
		this.town = town;
		this.country = country;
	}

	/** True when no address field carries a value (the address should then be omitted). */
	public boolean isEmpty()
	{
		return isBlank(street) && isBlank(buildingNumber) && isBlank(postcode)
				&& isBlank(town) && isBlank(country);
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	public String getStreet()
	{
		return street;
	}

	public void setStreet(String street)
	{
		this.street = street;
	}

	public String getBuildingNumber()
	{
		return buildingNumber;
	}

	public void setBuildingNumber(String buildingNumber)
	{
		this.buildingNumber = buildingNumber;
	}

	public String getPostcode()
	{
		return postcode;
	}

	public void setPostcode(String postcode)
	{
		this.postcode = postcode;
	}

	public String getTown()
	{
		return town;
	}

	public void setTown(String town)
	{
		this.town = town;
	}

	public String getCountry()
	{
		return country;
	}

	public void setCountry(String country)
	{
		this.country = country;
	}
}
