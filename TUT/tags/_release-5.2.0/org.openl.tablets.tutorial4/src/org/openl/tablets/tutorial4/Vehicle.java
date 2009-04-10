/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial4;

import org.openl.base.NamedThing;
import org.openl.meta.DoubleValue;

/**
 * @author snshor
 *
 */
public class Vehicle extends NamedThing 
{

	
	int year;
	String model;
	
	boolean hasAlarm;
	
	String type;
	boolean onHighTheftProbabilityList;
	String airbags;
	
	double price;
	
	String bodyType;
	
	String[] coverage;
	
	
	boolean hasRollBar;
	
/*** Rating attributes *******
 * To simplify demo logic we keep rating attributes together with basic
 * BOM. This approach is not recommended in real-life implementation 
 * 
 * */	

	int age;
	
	
	String theftRating;
	String injuryRating;
	
	String eligibility;
	
	
	DoubleValue score = new DoubleValue(0);
	
	public DoubleValue getFinalScore()
	{
		return new NamedDoubleValue("Score for " + getName(), score ).getResult();
	}
	

	/**
	 * @return Returns the bodyType.
	 */
	public String getBodyType() {
		return bodyType;
	}

	/**
	 * @param bodyType The bodyType to set.
	 */
	public void setBodyType(String bodyType) {
		this.bodyType = bodyType;
	}

	/**
	 * @return Returns the airbags.
	 */
	public String getAirbags() {
		return airbags;
	}

	/**
	 * @param airbags The airbags to set.
	 */
	public void setAirbags(String airbags) {
		this.airbags = airbags;
	}

	/**
	 * @return Returns the hasAlarm.
	 */
	public boolean isHasAlarm() {
		return hasAlarm;
	}

	/**
	 * @param hasAlarm The hasAlarm to set.
	 */
	public void setHasAlarm(boolean hasAlarm) {
		this.hasAlarm = hasAlarm;
	}

	/**
	 * @return Returns the model.
	 */
	public String getModel() {
		return model;
	}

	/**
	 * @param model The model to set.
	 */
	public void setModel(String model) {
		this.model = model;
	}


	/**
	 * @return Returns the onHighTheftProbabilityList.
	 */
	public boolean isOnHighTheftProbabilityList() {
		return onHighTheftProbabilityList;
	}

	/**
	 * @param onHighTheftProbabilityList The onHighTheftProbabilityList to set.
	 */
	public void setOnHighTheftProbabilityList(boolean onHighTheftProbabilityList) {
		this.onHighTheftProbabilityList = onHighTheftProbabilityList;
	}

	/**
	 * @return Returns the price.
	 */
	public double getPrice() {
		return price;
	}

	/**
	 * @param price The price to set.
	 */
	public void setPrice(double price) {
		this.price = price;
	}

	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return Returns the year.
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @param year The year to set.
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * @return Returns the theftRating.
	 */
	public String getTheftRating() {
		return theftRating;
	}

	/**
	 * @param theftRating The theftRating to set.
	 */
	public void setTheftRating(String theftRating) {
		this.theftRating = theftRating;
	}

	/**
	 * @return Returns the hasRollBar.
	 */
	public boolean isHasRollBar() {
		return hasRollBar;
	}

	/**
	 * @param hasRollBar The hasRollBar to set.
	 */
	public void setHasRollBar(boolean hasRollBar) {
		this.hasRollBar = hasRollBar;
	}

	/**
	 * @return Returns the injuryRating.
	 */
	public String getInjuryRating() {
		return injuryRating;
	}

	/**
	 * @param injuryRating The injuryRating to set.
	 */
	public void setInjuryRating(String injuryRating) {
		this.injuryRating = injuryRating;
	}

	/**
	 * @return Returns the eligibility.
	 */
	public String getEligibility() {
		return eligibility;
	}

	/**
	 * @param eligibility The eligibility to set.
	 */
	public void setEligibility(String eligibility) {
		this.eligibility = eligibility;
	}

	public int getAge()
	{
		return this.age;
	}

	public void setAge(int age)
	{
		this.age = age;
	}

	public String[] getCoverage()
	{
		return this.coverage;
	}

	public void setCoverage(String[] coverage)
	{
		this.coverage = coverage;
	}

	public DoubleValue getScore()
	{
		return this.score;
	}

	public void setScore(DoubleValue score)
	{
		this.score = score;
	}
	
}
