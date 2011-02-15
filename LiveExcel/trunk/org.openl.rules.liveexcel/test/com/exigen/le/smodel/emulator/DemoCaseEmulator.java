package com.exigen.le.smodel.emulator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.TableDesc.ColumnDesc;
import com.exigen.le.smodel.TableDesc.DataType;
import com.exigen.le.smodel.provider.ServiceModelProvider;

public class DemoCaseEmulator implements ServiceModelProvider {

	public ServiceModel create(String projectName, VersionDesc versionDesc) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Function> findFunctions(String projectName,
			VersionDesc versionDesc, List<Type> types) {
		
		List<Function> result = new ArrayList<Function>();
		Function serviceFunc = new Function("DemoCase2","Rating Algorithm", "rateAutoLE", "E8",
				 "Coverage", "E11", "Vehicle", "E12", "Driver", "E13", "Policy", "E14" );
		serviceFunc.setService(true);
		result.add(serviceFunc);
		
		result.add(new Function("DemoCase2", "Base Rate", "calcBaseRate","E24",
					"Coverage","E18"));
		result.add(new Function("DemoCase2", "FSB Rate.1", "calcFSBRate", "E18",
				"Coverage","E9", "Vehicle","E10","Rate Type","E11"));
		result.add(new Function("DemoCase2", "FSB Rate.2", "fsbRate", "E18",
				                "Zip Code", "E9", "Coverage Code", "E10", "Rate Type", "E11"));
		result.add(new Function("DemoCase2",  "Symbol Factor.1","calcSymbolRate", "E19",
				"Vehicle","E8","Coverage","E9"));
		result.add(new Function("DemoCase2", "Symbol Factor.2", "symbolRate", "E22",
				"Model Year","E8","Symbol","E9","Coverage Code","E10"));
		result.add(new Function("DemoCase2", "Symbol Factor.3", "symbolFactorRange", "E77",
				"Model Year","E74"));
		
		result.add(new Function("DemoCase2", "Model Year.1", "calcModelRate", "E16",
				"Vehicle","E8","Coverage","E9"));
		result.add(new Function("DemoCase2", "Vehicle Age.1", "calcVehicleAgeRate", "E17",
				"Vehicle","E8","Coverage","E9"));
		result.add(new Function("DemoCase2", "Vehicle Age.2", "vehicleAgeRate", "E19",
				"Model Year","E8","Effective Date","E9","Coverage Code","E10"));
		result.add(new Function("DemoCase2", "Deductible Factor.1", "calcDeductibleRate", "E15",
				"Coverage","E8"));
		result.add(new Function("DemoCase2", "Deductible Factor.2", "deductibleRate", "E12",
				"Deductible Amount","E8","Coverage Code","E9"));
		result.add(new Function("DemoCase2","Driver Record Points.1", "calcDriverRecordPointsRate", "E14",
				"Driver","E8"));
		result.add(new Function("DemoCase2","Driver Recrod Points.2", "driverRecordPointsRate", "E19",
				"Driving max record points","E8"));
		result.add(new Function("DemoCase2","Annual Mileage.1", "calcAnnualMileageRate", "E17",
				"Vehicle","E8","Coverage","E9"));
		result.add(new Function("DemoCase2","Annual Mileage.2", "annualMileageRange", "E35",
				"Annual milage","E28"));
		result.add(new Function("DemoCase2", "Class Factor.1", "calcClassRate", "E20",
				"Driver","E8","Coverage","E9"));
		result.add(new Function("DemoCase2", "Vehicle Use", "calcVehicleUseRate", "E26",
				"Vehicle","E20"));
		result.add(new Function("DemoCase2","Multiple Pocily Discount.1","calcMultiplePolicyDiscountRate", "E15",
				"Policy","E8"));
		result.add(new Function("DemoCase2","Multiple Pocily Discount.2","multiplePolicyDiscountRate","E18",
				"Multiple Policy Discount","E8","Previous Policy Code","E9"));
		result.add(new Function("DemoCase2","Persistency Discount.1","calcPersistencyDiscountRate", "E15",
				"Driver","E8"));
		result.add(new Function("DemoCase2", "Persistency Discount.2", "persistencyRange", "E22",
				"Policy Years","E19"));
		result.add(new Function("DemoCase2","Anti-theft Discount", "calcAntiTheftDiscountRate", "E27",
				"Coverage","E21"));
		result.add(new Function("DemoCase2", "Group Discount.1", "calcGroupDiscountRate", "E16",
				"Policy","E8"));
		result.add(new Function("DemoCase2", "Group Discount.2", "groupDiscountRate", "E26",
				"Driver Group","E8", "Insured Farm Bureau Number","E9", "Relationship","E10"));
		result.add(new Function("DemoCase2", "Commission Multiplier.1", "calcCommissionRate", "E14",
				"Coverage","E8"));
		result.add(new Function("DemoCase2", "Commission Multiplier.2","commissionRate", "E14",
				"Coverage Code", "E8"));
		result.add(new Function("DemoCase2","Multi Car.1", "calcMultiCarDiscountRate", "E17",
				"Coverage","E8", "Vehicle", "E9"));
		result.add(new Function("DemoCase2","Multi Car.2", "multiCarDiscountRate", "E19",
				"Coverage Code","E8", "Multi Car", "E9", "Company Car","E10"));
		result.add(new Function("DemoCase2", "Diminishing Deductible.1", "calcDiminishingDeductibleRate","E21",
				"Coverage","E8", "Vehicle", "E9", "Driver", "E10"));
		result.add(new Function("DemoCase2", "Diminishing Deductible.2", "diminishingDeductibleRate","E23",
				"Coverage Code", "E8", "Named NonOwned Policy", "E9", "Diminishing Deductible", "E10", "Driving Max Record Points", "E11", "Vehicle Type", "E12", "Deductible", "E13"));
		result.add(new Function("DemoCase2", "Total Loss Deductible.1", "calcTotalLossDeductibleRate","E19",
				"Coverage","E8", "Vehicle", "E9"));
		result.add(new Function("DemoCase2", "Total Loss Deductible.2", "totalLossDeductibleRate","E22",
				"Coverage Code", "E8", "Named NonOwned Policy", "E9", "total Loss Deductible Waiver", "E10",  "Vehicle Type", "E11", "Deductible", "E12"));
		result.add(new Function("DemoCase2","Expense Fees", "calcExpenseFeesRate", "E22",
				"Coverage","E16"));
		result.add(new Function("DemoCase2","Good Driver Discount", "calcGoodDriverDiscountRate", "E24",
				"Coverage","E17"));
		result.add(new Function("DemoCase2", "Term Adjustment Factor.1", "calcTermAdjustmentRate", "E15",
				"Policy","E8"));
		result.add(new Function("DemoCase2","Term Adjustment Factor.2", "termAdjustmentRate", "E16",
				"Effective Date", "E8","Experation Date","E9"));
		result.add(new Function("DemoCase2","Term Adjustment Factor.3","termAdjustmentRange", "E19",
				"Term Adjustment Months","E16"));
		return result;
	}

	public List<TableDesc> findTables(String projectName,
			VersionDesc versionDesc) {
		
		List<TableDesc> tables = new ArrayList<TableDesc>();
		
		tables.add(new TableDesc("BASERATE", DataType.DOUBLE,
				DataType.STRING));
		tables.add(new TableDesc("FSBFACTOR", DataType.DOUBLE,
				DataType.DOUBLE, DataType.STRING, DataType.STRING));
		tables.add(new TableDesc("FSBFREQUENCYBAND", DataType.DOUBLE,
				DataType.DOUBLE, DataType.STRING, DataType.STRING));
		
		tables.add(new TableDesc("SYMBOLFACTOR",DataType.DOUBLE,
				DataType.DOUBLE, DataType.STRING, DataType.STRING));
		
		tables.add(new TableDesc("modelYearFactor",DataType.DOUBLE,
				DataType.DOUBLE, DataType.STRING));
		
		List<ColumnDesc> cdl2 = new LinkedList<ColumnDesc>();
		cdl2.add(new ColumnDesc(DataType.DOUBLE, true));
		cdl2.add(new ColumnDesc(DataType.STRING));
		tables.add(new TableDesc("vehicleAgeFactor", cdl2, new ColumnDesc(DataType.DOUBLE)));
		
		tables.add(new TableDesc("deductibleFactor", DataType.DOUBLE,
				DataType.DOUBLE, DataType.STRING ));
		tables.add(new TableDesc("driverRecordPointsFactor", DataType.DOUBLE,
				DataType.DOUBLE));
		
		tables.add(new TableDesc("annualMileageFactor", DataType.DOUBLE,
				DataType.STRING, DataType.STRING));
		
		tables.add(new TableDesc("classFactor", DataType.DOUBLE,
				DataType.BOOLEAN, DataType.STRING, DataType.STRING,DataType.DOUBLE,DataType.STRING ,DataType.STRING));
		tables.add(new TableDesc("vehicleUseFactor", DataType.DOUBLE,
				DataType.STRING));
		
		TableDesc t1=new TableDesc("multiplePolicyDiscountFactor", DataType.DOUBLE,
				DataType.STRING);
		t1.setDefaultValue("1");
		tables.add(t1);

		t1 = new TableDesc("persistencyFactor", DataType.DOUBLE,
				DataType.STRING);
		t1.setDefaultValue("1");
		tables.add(t1);
		
		t1= new TableDesc("antiTheftDiscountFactor", DataType.DOUBLE,
			DataType.STRING);
		t1.setDefaultValue("1");
		tables.add(t1);
		
		t1 = new TableDesc("groupDiscountFactor", DataType.DOUBLE,
				DataType.STRING);
		t1.setDefaultValue("1");
		tables.add(t1);
		
		tables.add(new TableDesc("multiCarDiscountFactor", DataType.DOUBLE, DataType.STRING));
		
		tables.add(new TableDesc("diminishingDeductibleFactor",DataType.DOUBLE,
				DataType.DOUBLE, DataType.STRING));
		tables.add(new TableDesc("totalLossDeductibleFactor",DataType.DOUBLE,
				DataType.DOUBLE, DataType.STRING));
		tables.add(new TableDesc("expenseFees",DataType.DOUBLE,
				DataType.STRING));
		tables.add(new TableDesc("goodDriverDiscountFactor", DataType.DOUBLE,
				DataType.STRING));
		tables.add(new TableDesc("termAdjustmentFactor", DataType.DOUBLE,
				DataType.STRING));
		return tables;
	}

	public List<Type> findTypes(String projectName, VersionDesc versionDesc) {

		List<Type> result = new LinkedList<Type>();
		
		Type vehicle = new Type("Vehicle", true);
		List<MappedProperty> vProperties = new LinkedList<MappedProperty>(); 
		vProperties.add(new MappedProperty("zipCode", Type.DOUBLE));
		vProperties.add(new MappedProperty("antiTheftDeviceCode", Type.STRING));
		vProperties.add(new MappedProperty("estimatedAnnualDistance", Type.DOUBLE));
		vProperties.add(new MappedProperty("modelYear", Type.DOUBLE));
		vProperties.add(new MappedProperty("rateEffectiveDate", Type.DATE));
		vProperties.add(new MappedProperty("vehicleUsageCode", Type.STRING));
		vProperties.add(new MappedProperty("collisionSymbol", Type.DOUBLE));
		vProperties.add(new MappedProperty("comprehensiveSymbol", Type.DOUBLE));
		vProperties.add(new MappedProperty("multiCar", Type.BOOLEAN));
		vProperties.add(new MappedProperty("nonOwnedPolicy", Type.BOOLEAN));
		vehicle.setChilds(vProperties);
		result.add(vehicle);
		
		
		Type coverage = new Type("Coverage", true);
		List<MappedProperty> cP = new LinkedList<MappedProperty>();
		cP.add(new MappedProperty("coverageCode",Type.STRING));
		cP.add(new MappedProperty("dedactibleAmount",Type.DOUBLE));
		coverage.setChilds(cP);
		result.add(coverage);
		
		Type driver = new Type("Driver", true);
		List<MappedProperty> dP = new LinkedList<MappedProperty>();
		dP.add(new MappedProperty("driverTypeCode", Type.STRING));
		dP.add(new MappedProperty("maxPoints", Type.DOUBLE));
		dP.add(new MappedProperty("farmBureauMemberIndicator", Type.BOOLEAN));
		dP.add(new MappedProperty("gender", Type.STRING));
		dP.add(new MappedProperty("goodEliteDriverCode", Type.STRING));
		dP.add(new MappedProperty("goodStudentIndicator", Type.BOOLEAN));
		dP.add(new MappedProperty("maritalStatusCode", Type.STRING));
		dP.add(new MappedProperty("occupationCode", Type.STRING));
		dP.add(new MappedProperty("relationToInsuredCode", Type.STRING));
		dP.add(new MappedProperty("yearsLisensed", Type.DOUBLE));
		dP.add(new MappedProperty("yearsOfCoverage", Type.DOUBLE));
		driver.setChilds(dP);
		result.add(driver);
		
		Type policy = new Type("Policy", true);
		List<MappedProperty> pP = new LinkedList<MappedProperty>();
		pP.add(new MappedProperty("effectiveDate", Type.DATE));
		pP.add(new MappedProperty("expirationDate", Type.DATE));
		pP.add(new MappedProperty("multiplePolicyDiscount", Type.BOOLEAN));
		policy.setChilds(pP);
		result.add(policy);
		
		return result;
	}

}
