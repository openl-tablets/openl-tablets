package com.exigen.le.calculation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.servicedescr.evaluator.PropertyRetriever;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.provider.ServiceModelJAXB;

public class MappingPropertyTest implements PropertyRetriever{
	
	
	
	
	
	@Test
	public void mappedTest() throws Exception {
		Driver driver = fillDriver();
		Coverage coverage = fillCoverage();
		Policy policy = fillPolicy();
		Vehicle vehicle = fillVehicle();
		
		// Restore regular SM provider(from xml), which can be overriden by other tests
        ServiceModelJAXB provider = new ServiceModelJAXB(new File("./test-resources/LERepository/DemoCase2Mapped"));
        LiveExcel le = new LiveExcel(provider);
	
		List args = new LinkedList();
		args.add(coverage);
		args.add(vehicle);
		args.add(driver);
		args.add(policy);
		
		
        ThreadEvaluationContext.getEnvProperties().clear();
		LE_Value result = le.calculate("rateAutoLE".toUpperCase(), args);
		String s = SMHelper.valueToString(result);
		assertEquals("343.07",s);
		
		
		
	}
	
	
	Driver fillDriver() throws Exception{
		Driver bean = new Driver();
		Map<String, Object> dMap = new HashMap<String, Object>();
		dMap.put("driverTypeCode", "Primary");
		dMap.put("maxPoints",0 );
		dMap.put("farmBureauMemberIndicator", true);
		dMap.put("gender","Female" );
		dMap.put("goodEliteDriverCode", "Good Driver");
		dMap.put("goodStudentIndicator",false );
		dMap.put("maritalStatusCode","Single" );
		dMap.put("occupationCode","FIRE FIGHTER" );
		dMap.put("relationToInsuredCode","IN" );
		//dMap.put("yearsOfCoverage",3);
		dMap.put("yearsLisensed",0);
		
	    BeanUtils.copyProperties(bean, dMap);
		return bean;
	}
	
	
	Coverage fillCoverage() throws Exception{
		Coverage bean = new Coverage();
		Map<String, Object> cMap = new HashMap<String, Object>();
	    cMap.put("coverageCodeX","BASE" );
	    cMap.put("dedactibleAmountX",200 );
	    BeanUtils.copyProperties(bean, cMap);
	    return bean;
	}
	
	Vehicle fillVehicle() throws Exception{
		Vehicle bean = new Vehicle();
		Map<String, Object> vMap = new HashMap<String, Object>();
		vMap.put("zipCode", "90004");
		vMap.put("antiTheftDeviceCode", "PASSIVE");
		vMap.put("estimatedAnnualDistance", 14999);
		vMap.put("modelYear", 2000);
		vMap.put("rateEffectiveDate", new GregorianCalendar(2002, 7, 1));
		vMap.put("vehicleUsageCode", "LONG COMMUTE");
		vMap.put("collisionSymbol","32" );
		vMap.put("comprehensiveSymbol", "31");
		vMap.put("multiCar", true);
		vMap.put("nonOwnedPolicy",false );
		
		BeanUtils.copyProperties(bean, vMap);
		return bean;
		
	}
	
	Policy fillPolicy() throws Exception {
		Policy bean = new Policy();
		
		Map<String, Object> pMap = new HashMap<String, Object>();
		pMap.put("effectiveDate", new GregorianCalendar(2009, 6, 20));
		pMap.put("expirationDate", new GregorianCalendar(2009, 6, 20));
		pMap.put("multiplePolicyDiscount",false );
		
	    BeanUtils.copyProperties(bean, pMap);
	    return bean;
	    
	}
	
	public static class Driver {
		
		String driverTypeCode;
		Double maxPoints;
		Boolean farmBureauMemberIndicator;
		String gender;
		String goodEliteDriverCode;
		Boolean goodStudentIndicator;
		String maritalStatusCode;
		String occupationCode;
		String relationToInsuredCode;
		Double yearsLisensed;
//		Double yearsOfCoverage;
		/**
		 * @return the driverTypeCode
		 */
		public String getDriverTypeCode() {
			return driverTypeCode;
		}
		/**
		 * @param driverTypeCode the driverTypeCode to set
		 */
		public void setDriverTypeCode(String driverTypeCode) {
			this.driverTypeCode = driverTypeCode;
		}
		/**
		 * @return the maxPoints
		 */
		public Double getMaxPoints() {
			return maxPoints;
		}
		/**
		 * @param maxPoints the maxPoints to set
		 */
		public void setMaxPoints(Double maxPoints) {
			this.maxPoints = maxPoints;
		}
		/**
		 * @return the farmBureauMemberIndicator
		 */
		public Boolean getFarmBureauMemberIndicator() {
			return farmBureauMemberIndicator;
		}
		/**
		 * @param farmBureauMemberIndicator the farmBureauMemberIndicator to set
		 */
		public void setFarmBureauMemberIndicator(Boolean farmBureauMemberIndicator) {
			this.farmBureauMemberIndicator = farmBureauMemberIndicator;
		}
		/**
		 * @return the gender
		 */
		public String getGender() {
			return gender;
		}
		/**
		 * @param gender the gender to set
		 */
		public void setGender(String gender) {
			this.gender = gender;
		}
		/**
		 * @return the goodEliteDriverCode
		 */
		public String getGoodEliteDriverCode() {
			return goodEliteDriverCode;
		}
		/**
		 * @param goodEliteDriverCode the goodEliteDriverCode to set
		 */
		public void setGoodEliteDriverCode(String goodEliteDriverCode) {
			this.goodEliteDriverCode = goodEliteDriverCode;
		}
		/**
		 * @return the goodStudentIndicator
		 */
		public Boolean getGoodStudentIndicator() {
			return goodStudentIndicator;
		}
		/**
		 * @param goodStudentIndicator the goodStudentIndicator to set
		 */
		public void setGoodStudentIndicator(Boolean goodStudentIndicator) {
			this.goodStudentIndicator = goodStudentIndicator;
		}
		/**
		 * @return the maritalStatusCode
		 */
		public String getMaritalStatusCode() {
			return maritalStatusCode;
		}
		/**
		 * @param maritalStatusCode the maritalStatusCode to set
		 */
		public void setMaritalStatusCode(String maritalStatusCode) {
			this.maritalStatusCode = maritalStatusCode;
		}
		/**
		 * @return the occupationCode
		 */
		public String getOccupationCode() {
			return occupationCode;
		}
		/**
		 * @param occupationCode the occupationCode to set
		 */
		public void setOccupationCode(String occupationCode) {
			this.occupationCode = occupationCode;
		}
		/**
		 * @return the relationToInsuredCode
		 */
		public String getRelationToInsuredCode() {
			return relationToInsuredCode;
		}
		/**
		 * @param relationToInsuredCode the relationToInsuredCode to set
		 */
		public void setRelationToInsuredCode(String relationToInsuredCode) {
			this.relationToInsuredCode = relationToInsuredCode;
		}
		/**
		 * @return the yearsLisensed
		 */
		public Double getYearsLisensed() {
			return yearsLisensed;
		}
		/**
		 * @param yearsLisensed the yearsLisensed to set
		 */
		public void setYearsLisensed(Double yearsLisensed) {
			this.yearsLisensed = yearsLisensed;
		}
//		/**
//		 * @return the yearsOfCoverage
//		 */
//		public Double getYearsOfCoverage() {
//			return yearsOfCoverage;
//		}
//		/**
//		 * @param yearsOfCoverage the yearsOfCoverage to set
//		 */
//		public void setYearsOfCoverage(Double yearsOfCoverage) {
//			this.yearsOfCoverage = yearsOfCoverage;
//		}
	}
	
	public static class Coverage {
		String coverageCodeX;
		Double dedactibleAmountX;
		/**
		 * @return the coverageCodeX
		 */
		public String getCoverageCodeX() {
			return coverageCodeX;
		}
		/**
		 * @param coverageCodeX the coverageCodeX to set
		 */
		public void setCoverageCodeX(String coverageCode) {
			this.coverageCodeX = coverageCode;
		}
		/**
		 * @return the dedactibleAmount
		 */
		public Double getDedactibleAmountX() {
			return dedactibleAmountX;
		}
		/**
		 * @param dedactibleAmount the dedactibleAmount to set
		 */
		public void setDedactibleAmountX(Double dedactibleAmount) {
			this.dedactibleAmountX = dedactibleAmount;
		}
	}
	
	public static class Policy {
		Calendar effectiveDate;
		Calendar expirationDate;
		Boolean multiplePolicyDiscount;
		/**
		 * @return the effectiveDate
		 */
		public Calendar getEffectiveDate() {
			return effectiveDate;
		}
		/**
		 * @param effectiveDate the effectiveDate to set
		 */
		public void setEffectiveDate(Calendar effectiveDate) {
			this.effectiveDate = effectiveDate;
		}
		/**
		 * @return the expirationDate
		 */
		public Calendar getExpirationDate() {
			return expirationDate;
		}
		/**
		 * @param expirationDate the expirationDate to set
		 */
		public void setExpirationDate(Calendar expirationDate) {
			this.expirationDate = expirationDate;
		}
		/**
		 * @return the multiplePolicyDiscount
		 */
		public Boolean getMultiplePolicyDiscount() {
			return multiplePolicyDiscount;
		}
		/**
		 * @param multiplePolicyDiscount the multiplePolicyDiscount to set
		 */
		public void setMultiplePolicyDiscount(Boolean multiplePolicyDiscount) {
			this.multiplePolicyDiscount = multiplePolicyDiscount;
		}
	}
	
	public class Vehicle {
		
		Double zipCode;
		String antiTheftDeviceCode;
		Double estimatedAnnualDistance;
		Double modelYear;
		Calendar rateEffectiveDate;
		String vehicleUsageCode;
		Double collisionSymbol;
		Double comprehensiveSymbol;
		Boolean multiCar;
		Boolean nonOwnedPolicy;
		/**
		 * @return the zipCode
		 */
		public Double getZipCode() {
			return zipCode;
		}
		/**
		 * @param zipCode the zipCode to set
		 */
		public void setZipCode(Double zipCode) {
			this.zipCode = zipCode;
		}
		/**
		 * @return the antiTheftDeviceCode
		 */
		public String getAntiTheftDeviceCode() {
			return antiTheftDeviceCode;
		}
		/**
		 * @param antiTheftDeviceCode the antiTheftDeviceCode to set
		 */
		public void setAntiTheftDeviceCode(String antiTheftDeviceCode) {
			this.antiTheftDeviceCode = antiTheftDeviceCode;
		}
		/**
		 * @return the estimatedAnnualDistance
		 */
		public Double getEstimatedAnnualDistance() {
			return estimatedAnnualDistance;
		}
		/**
		 * @param estimatedAnnualDistance the estimatedAnnualDistance to set
		 */
		public void setEstimatedAnnualDistance(Double estimatedAnnualDistance) {
			this.estimatedAnnualDistance = estimatedAnnualDistance;
		}
		/**
		 * @return the modelYear
		 */
		public Double getModelYear() {
			return modelYear;
		}
		/**
		 * @param modelYear the modelYear to set
		 */
		public void setModelYear(Double modelYear) {
			this.modelYear = modelYear;
		}
		/**
		 * @return the rateEffectiveDate
		 */
		public Calendar getRateEffectiveDate() {
			return rateEffectiveDate;
		}
		/**
		 * @param rateEffectiveDate the rateEffectiveDate to set
		 */
		public void setRateEffectiveDate(Calendar rateEffectiveDate) {
			this.rateEffectiveDate = rateEffectiveDate;
		}
		/**
		 * @return the vehicleUsageCode
		 */
		public String getVehicleUsageCode() {
			return vehicleUsageCode;
		}
		/**
		 * @param vehicleUsageCode the vehicleUsageCode to set
		 */
		public void setVehicleUsageCode(String vehicleUsageCode) {
			this.vehicleUsageCode = vehicleUsageCode;
		}
		/**
		 * @return the collisionSymbol
		 */
		public Double getCollisionSymbol() {
			return collisionSymbol;
		}
		/**
		 * @param collisionSymbol the collisionSymbol to set
		 */
		public void setCollisionSymbol(Double collisionSymbol) {
			this.collisionSymbol = collisionSymbol;
		}
		/**
		 * @return the comprehensiveSymbol
		 */
		public Double getComprehensiveSymbol() {
			return comprehensiveSymbol;
		}
		/**
		 * @param comprehensiveSymbol the comprehensiveSymbol to set
		 */
		public void setComprehensiveSymbol(Double comprehensiveSymbol) {
			this.comprehensiveSymbol = comprehensiveSymbol;
		}
		/**
		 * @return the multiCar
		 */
		public Boolean getMultiCar() {
			return multiCar;
		}
		/**
		 * @param multiCar the multiCar to set
		 */
		public void setMultiCar(Boolean multiCar) {
			this.multiCar = multiCar;
		}
		/**
		 * @return the nonOwnedPolicy
		 */
		public Boolean getNonOwnedPolicy() {
			return nonOwnedPolicy;
		}
		/**
		 * @param nonOwnedPolicy the nonOwnedPolicy to set
		 */
		public void setNonOwnedPolicy(Boolean nonOwnedPolicy) {
			this.nonOwnedPolicy = nonOwnedPolicy;
		}
	}
	
	

		public Object retrieveProperty(Object source, String propertyName,
				int index) throws Exception {
			return new Double(3);
		}
	    
	    // We should clear all created temp files manually because JUnit terminates
	    // JVM incorrectly and finalization methods are not executed
	    @After
	    public void finalize() {
	        try {
	            ProjectLoader.reset();
	            FileUtils.deleteDirectory(ProjectLoader.getTempDir());
	        } catch (IOException e) {
	            e.printStackTrace();
	            assertFalse(true);
	        }
	    }
}
