/*
 * Copyright (c) 2004 Exigen Properties, Inc. and/or affiliates.
 * All Rights Reserved.
 *  $Header: /cvs/src/VisiFlowInt/Products/B302/Prototypes/LiveExcel/le-init-db/src/main/java/com/exigen/le/init/InitDbApplication.java,v 1.15 2009/07/17 08:48:45 DSapunovs Exp $
 */
package com.exigen.le.democase;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;


public class BuildDemoCaseObjects {
    
    
    public static  List<Vehicle>  createVehicles() {
        
    	List<Vehicle> result = new ArrayList<Vehicle>();
        // 6/1/2009
        Calendar effectiveDate = Calendar.getInstance();
        effectiveDate.set(Calendar.YEAR, 2009);
        effectiveDate.set(Calendar.MONTH, 5);
        effectiveDate.set(Calendar.DAY_OF_MONTH, 1);

        createVehicle(result, 1,  2000, 31, 32, new GregorianCalendar(2002, 6, 1), 14999d, "LONG COMMUTE", "PASSIVE", "90004", "Oakwood", true, false);
        createVehicle(result, 2,  2007, 32, 31, new GregorianCalendar(2009, 5, 4), 6000d, "SHORT COMMUTE", "PASSIVE", "90003", "Los Angeles", true, true);
        createVehicle(result, 3,  2008, 32, 31, new GregorianCalendar(2009, 5, 4), 6000d, "SHORT COMMUTE", "PASSIVE", "90004", "Oakwood", true, true);
        createVehicle(result, 4,  2008, 31, 30, effectiveDate, 0d,     "PLEASURE USE",  "ALARM",   "90210", "Beverly Hills", true, false);
        createVehicle(result, 5,  2008, 60, 47, effectiveDate, 11660d, "SHORT COMMUTE", "PASSIVE", "90210", "Beverly Hills", true, false);
        createVehicle(result, 6,  2008, 60, 47, effectiveDate, 11660d, "SHORT COMMUTE", "PASSIVE", "90210", "Beverly Hills", true, false);
        createVehicle(result, 7,  2008, 60, 47, effectiveDate, 11660d, "SHORT COMMUTE", "ACTIVE",  "90210", "Beverly Hills", true, false);
        createVehicle(result, 8,  2008, 60, 47, effectiveDate, 13740d, "LONG COMMUTE",  "PASSIVE", "90210", "Beverly Hills", true, false);
        createVehicle(result, 9,  2008, 60, 47, effectiveDate, 15300d, "BUSINESS USE",  "ALARM",   "90210", "Beverly Hills", true, false);
        createVehicle(result, 10, 2008, 60, 47, effectiveDate, 13740d, "SHORT COMMUTE", "PASSIVE", "90742", "Sunset Beach", true, false);
        createVehicle(result, 11, 2008, 48, 38, effectiveDate, 13740d, "SHORT COMMUTE", "PASSIVE", "90808", "Long Beach",   true, false);
        createVehicle(result, 12, 2008, 60, 47, effectiveDate, 15300d, "LONG COMMUTE",  "ACTIVE",  "91020", "Montrose",     true, false);
        createVehicle(result, 13, 2008, 48, 38, effectiveDate, 13740d, "SHORT COMMUTE", "PASSIVE", "91101", "Los Angeles",   true, false);
        createVehicle(result, 14, 2008, 60, 47, effectiveDate, 13740d, "SHORT COMMUTE", "ALARM",   "91108", "Los Angeles",   true, false);
        createVehicle(result, 15, 2001, 34, 28, effectiveDate, 30900d, "LONG COMMUTE",  "PASSIVE", "90210", "Beverly Hills", true, false);
        createVehicle(result, 16, 2001, 34, 28, effectiveDate, 15300d, "LONG COMMUTE",  "PASSIVE", "90210", "Beverly Hills", true, false);
        createVehicle(result, 17, 2001, 34, 28, effectiveDate, 15000d, "PLEASURE USE",  "PASSIVE", "90742", "Sunset Beach",  true, false);
        createVehicle(result, 18, 2001, 34, 28, effectiveDate, 20000d, "PLEASURE USE",  "PASSIVE", "90808", "Long Beach",    true, false);
        createVehicle(result, 19, 2009, 32, 27, effectiveDate, 15000d, "PLEASURE USE",  "PASSIVE", "90210", "Beverly Hills", true, false);
        createVehicle(result, 20, 2000, 32, 26, effectiveDate, 22580d, "LONG COMMUTE",  "PASSIVE", "94505", "Discovery Bay", true, false);
        
        return result;
    }
    

    private static void createVehicle(
    		List<Vehicle> vehicles,
            int id,
            Integer modelYear,
            int vehCompSymbol,
            int vehCollSymbol,
            Calendar rateEffectiveDt,
            Double estimatedAnnualDistance,
            String vehUseCd,
            String antiTheftDeviceCd,
            String zipCode,
            String city,
            boolean multiCarDiscount,
            boolean nonOwned) {

        Vehicle vehicle = new Vehicle();
        
        if (modelYear != null) {
            vehicle.setModelYear(new Double(modelYear.doubleValue()));
        }
        
        vehicle.setCollisionSymbol(new Double(vehCollSymbol));
        vehicle.setComprehensiveSymbol(new Double(vehCompSymbol));
        vehicle.setRateEffectiveDate(rateEffectiveDt);
        if (estimatedAnnualDistance != null) {
            vehicle.setEstimatedAnnualDistance(estimatedAnnualDistance);
        }
        vehicle.setVehicleUsageCode(vehUseCd);
        vehicle.setAntiTheftDeviceCode(antiTheftDeviceCd);
    
        vehicle.setZipCode(new Double(zipCode));
        vehicle.setMultiCar( multiCarDiscount );
        vehicle.setNonOwnedPolicy( nonOwned );
 
        
        vehicles.add(vehicle);
    }
    
    
    public  static List<Coverage> createCoverages() {
    	List<Coverage> result = new ArrayList<Coverage>();
        createCoverage(result,  1, "BASE", 200);
        createCoverage(result,  2, "BASE", 250);
        createCoverage(result,  3, "COLL", 200);
        createCoverage(result,  4, "BASE", 250);
        createCoverage(result,  5, "COLL", 200);
        createCoverage(result,  6, "BASE", 250);
        createCoverage(result,  7, "COLL", 250);
        createCoverage(result,  8, "BASE", 1000);
        createCoverage(result,  9, "COLL", 1000);
        createCoverage(result, 10, "BASE", 1000);
        return result;
    }
    
    private static void createCoverage(
            List<Coverage> coverages,
            int id,
            String code,
            double deductibleAmount) {
        Coverage coverage = new Coverage();
        coverage.setCoverageCode(code);
        coverage.setDedactibleAmount(new Double(deductibleAmount) );
        coverages.add(coverage);
    }
    

    public static List<Driver> createDrivers() {
    	List<Driver> result = new ArrayList<Driver>();
    	
        createDriver(result, 1, 0,  "Female", 0,  "Single",  3, "FIRE FIGHTER", "Primary",true, false, "Good Driver");
        
        createDriver(result, 2, 0,  "Male", 2, "Married",  2, "FIRE FIGHTER","Primary", true, true, "Good Driver");
        createDriver(result, 3, 1,  "Male", 5, "Single",  2, "VETERINARIAN","Occasional1",true, false, "Good Driver");
        createDriver(result, 4, 0,  "Female", 10, "Single",  2, "POLICE OFFICER","Primary",true, true, "Good Driver");
            
        createDriver(result, 5, 0,  "Female", 30, "Single",  0, "Engeneer","Primary", false, true, "Good Driver");
        createDriver(result, 6, 2,  "Female",  3, "Single",  0, "Musician","Primary", false, false, "Elite Good Driver");
        createDriver(result, 7, 5,  "Male",    3, "Married", 0, "Dentist","Primary", false, false, "None");
        createDriver(result, 8, 10, "Female",  9, "Single",  0, "Management","Primary", false, false, "Good Driver");
        createDriver(result, 9, 11, "Female",  5, "Single",  0, "Homemaker","Primary", false, false, "None");
        createDriver(result, 10, 15, "Male",    3, "Single",  0, "Mechanic","Primary", false, false, "None");
        createDriver(result, 11, 5,  "Female", 30, "Single",  0, "Engeneer","Primary", false, false, "None");
        createDriver(result, 12, 0,  "Female",  3, "Single",  3, "Musician","Primary", false, false, "None");
        createDriver(result, 13, 0,  "Male",    3, "Married", 0, "Dentist","Primary", false, false, "None");
        createDriver(result, 14, 3, "Female",  9, "Married", 0, "Management","Primary", false, true, "Elite Good Driver");
        createDriver(result, 15, 8, "Male",    3, "Single",  0, "Mechanic","Primary", false, false, "None");
        createDriver(result, 16, 7, "Female",  5, "Single",  0, "Homemaker","Primary", false, false, "None");
        createDriver(result, 17, 6, "Male",    6, "Single",  0, "Mechanic","Primary", false, false, "Good Driver");
        createDriver(result, 18, 6, "Female", 30, "Single",  0, "Engeneer","Primary", false, false, "None");
        createDriver(result, 19, 2, "Female",  9, "Single",  0, "Musician","Primary", false, false, "None");
        createDriver(result, 20, 2, "Female", 32, "Married", 0, "Accountant","Primary", false, false, "Elite Good Driver");
        createDriver(result, 21, 0, "Female", 24, "Married", 0, "Homemaker","Primary", false, false, "Elite Good Driver");
        return result;
    }
    

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat( "MM.dd.yyyy" ); 
    

    private static void createDriver(
            List<Driver> drivers,
            int id,
            int points,
            String genderCd,
            double yearsLicensed,
            String maritalStatusCd,
            int yearsCoverage,
            String occupationCd,
            String driverTypeCd,
            boolean farmBureauMemberInd,
            boolean goodStudentInd,
            String goodEliteDriverCd) {

        String relationToInsuredCd = "IN";
        
        Driver driver = new Driver();
        driver.setMaxPoints(new Double(points));
        driver.setGender(genderCd);
        driver.setYearsLisensed(yearsLicensed);
        driver.setMaritalStatusCode(maritalStatusCd);
        driver.setDriverTypeCode(driverTypeCd);
        driver.setGoodStudentIndicator(goodStudentInd);
        driver.setYearsOfCoverage(new Double(yearsCoverage));
        driver.setOccupationCode(occupationCd);
        driver.setFarmBureauMemberIndicator(farmBureauMemberInd);
        driver.setRelationToInsuredCode(relationToInsuredCd);
        driver.setGoodEliteDriverCode(goodEliteDriverCd);
        
        drivers.add(driver);
    }
    

    public static List<Policy> createPolicies() {
    	List<Policy> result = new ArrayList<Policy>();
    	
        createPolicy(result,  1, true,  "05.13.2009", "07.13.2009");
        createPolicy(result,  2, false, "06.20.2009", "06.20.2009");
        
        createPolicy(result,  3, false, "12.20.2008", "12.20.2009");
        createPolicy(result,  4, false, "01.11.2008", "01.11.2009");
        createPolicy(result,  5, false, "01.11.2008", "01.11.2009");
        createPolicy(result,  6, false, "12.20.2008", "12.20.2009");
        createPolicy(result,  7, true,  "12.20.2008", "12.20.2009");
        createPolicy(result,  8, false, "12.20.2008", "12.20.2009");
        createPolicy(result,  9, false, "01.11.2008", "01.11.2009");
        createPolicy(result, 10, false, "12.20.2008", "12.20.2009");
        createPolicy(result, 11, false, "01.11.2008", "01.11.2009");
        createPolicy(result, 12, true,  "12.20.2008", "12.20.2009");
        createPolicy(result, 13, false, "12.20.2008", "12.20.2009");
        createPolicy(result, 14, true,  "01.11.2008", "01.11.2009");
        createPolicy(result, 15, false, "01.11.2008", "01.11.2009");
        createPolicy(result, 16, false, "01.11.2008", "01.11.2009");
        createPolicy(result, 17, false, "12.20.2008", "12.20.2009");
        createPolicy(result, 18, false, "01.11.2008", "01.11.2009");
        createPolicy(result, 19, true,  "01.11.2008", "01.11.2009");
        return result;
    }
    

    private static void createPolicy(
            List<Policy> policies,
            long id,
            boolean multiPolicy,
            String effectiveDate,
            String expirationDate) {
        Policy policy = new Policy();
        policy.setMultiplePolicyDiscount(multiPolicy);

        if (effectiveDate != null && expirationDate != null) {
            
            try {
                Calendar effective = Calendar.getInstance();
                effective.setTime( FORMAT.parse(effectiveDate) );
                
                Calendar expiration = Calendar.getInstance();
                expiration.setTime( FORMAT.parse(expirationDate) );
                
                policy.setEffectiveDate(effective);
                policy.setExpirationDate(expiration);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        policies.add(policy);
    }
}
