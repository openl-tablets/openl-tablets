package com.exigen.le.smodel.emulator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.TableDesc.DataType;
import com.exigen.le.smodel.provider.ServiceModelProvider;

public class PYDEmulator implements ServiceModelProvider {

	public ServiceModel create() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Function> findFunctions(List<Type> types) {
		
		List<Function> result = new ArrayList<Function>();
		Function serviceFunc = new Function("PAYD Rater GJD.xlsm","Rate Calculator", "PYDPremiumCalculator", "D12",
				 "Driver", "D6", "Vehicle", "D7", "Policy", "D8", "string", "D9" );
		serviceFunc.setService(true);
		result.add(serviceFunc);
		
		return result;
	}

	public List<TableDesc> findTables() {
		
		List<TableDesc> tables = new ArrayList<TableDesc>();
		
		tables.add(new TableDesc("tBASERATE", DataType.DOUBLE,
				DataType.STRING));
		return tables;
	}

	public List<Type> findTypes() {

		List<Type> result = new LinkedList<Type>();
		
		Type extras = new Type("Extras", true);
		List<MappedProperty> eProperties = new ArrayList<MappedProperty>(); 
		eProperties.add(new MappedProperty("Extra_ID",Type.STRING));
		extras.setChilds(eProperties);
		
		
		Type vehicle = new Type("Vehicle", true);
		List<MappedProperty> vProperties = new LinkedList<MappedProperty>();
		MappedProperty key = new MappedProperty("VIN",Type.STRING);
		key.setKey(true);
		vProperties.add(key);
		vProperties.add(new MappedProperty("RedbookCode",Type.STRING));
		vProperties.add(new MappedProperty("Day_Time_Suburb",Type.STRING));
		vProperties.add(new MappedProperty("Day_Time_Parking",Type.STRING));
		vProperties.add(new MappedProperty("Night_Suburb",Type.STRING));
		vProperties.add(new MappedProperty("NParking",Type.STRING));
		vProperties.add(new MappedProperty("Type_of_Use",Type.STRING));
		vProperties.add(new MappedProperty("offRoad",Type.BOOLEAN));
		vProperties.add(new MappedProperty("Annual_km",Type.DOUBLE));
		vProperties.add(new MappedProperty("Colour",Type.STRING));
		vProperties.add(new MappedProperty("Security",Type.STRING));
		vProperties.add(new MappedProperty("CarKit",Type.BOOLEAN));
		vProperties.add(new MappedProperty("Currently_Insured",Type.BOOLEAN));
		vProperties.add(new MappedProperty("Financed",Type.STRING));
		vProperties.add(new MappedProperty("CarHireOp",Type.BOOLEAN));
		vProperties.add(new MappedProperty("Windscreen_Option",Type.BOOLEAN));
		vProperties.add(new MappedProperty("RefSource",Type.STRING));
		vProperties.add(new MappedProperty("Vehicle_Year",Type.STRING));
		vProperties.add(new MappedProperty("Make",Type.STRING));
		vProperties.add(new MappedProperty("Model",Type.STRING));
		vProperties.add(new MappedProperty("Vehicle_Details",Type.STRING));
		vProperties.add(new MappedProperty("State",Type.STRING));
		vProperties.add(new MappedProperty("Day_Postcode",Type.STRING));
		vProperties.add(new MappedProperty("Night_Postcode",Type.STRING));
		vProperties.add(new MappedProperty("Mod_Value",Type.STRING));
		vProperties.add(new MappedProperty("extras",extras,true,true));
		vProperties.add(new MappedProperty("Filler",Type.STRING));
		vehicle.setChilds(vProperties);
		result.add(vehicle);
		
		
		Type coverage = new Type("Coverage", true);
		List<MappedProperty> cP = new LinkedList<MappedProperty>();
		cP.add(new MappedProperty("coverType",Type.STRING));
		coverage.setChilds(cP);
		result.add(coverage);
		
		Type driver = new Type("Driver", true);
		List<MappedProperty> dP = new LinkedList<MappedProperty>();
		
		dP.add(new MappedProperty("License_Number", Type.STRING));
		dP.add(new MappedProperty("P_DOB", Type.DATE));
		dP.add(new MappedProperty("Pgender", Type.STRING));
		dP.add(new MappedProperty("PMarital Status", Type.STRING));
		dP.add(new MappedProperty("P_Age", Type.DOUBLE));
		dP.add(new MappedProperty("PYear_Licence_obtained", Type.DOUBLE));
		dP.add(new MappedProperty("Claim_5yr", Type.DOUBLE));
		dP.add(new MappedProperty("Own_Other_Vehicle", Type.BOOLEAN));
		dP.add(new MappedProperty("License_Suspension", Type.DOUBLE));
		dP.add(new MappedProperty("SeniorsCard", Type.DOUBLE));
		dP.add(new MappedProperty("Y_DOB", Type.DATE));
		dP.add(new MappedProperty("YGender", Type.STRING));
		dP.add(new MappedProperty("YMarital_Status", Type.STRING));
		dP.add(new MappedProperty("Yage", Type.DOUBLE));
		driver.setChilds(dP);
		result.add(driver);
		
		Type policy = new Type("Policy", true);
		List<MappedProperty> pP = new LinkedList<MappedProperty>();
		pP.add(new MappedProperty("Policy_Number", Type.STRING));
		pP.add(new MappedProperty("KM_Purchased", Type.DOUBLE));
		pP.add(new MappedProperty("Excess", Type.DOUBLE));
		pP.add(new MappedProperty("SumInsured", Type.DOUBLE));
		pP.add(new MappedProperty("RateDate", Type.DATE));
		pP.add(new MappedProperty("DriverAgeRestriction", Type.STRING));
		policy.setChilds(pP);
		result.add(policy);
		
		return result;
	}

    public File getProjectLocation() {
        // TODO Auto-generated method stub
        return null;
    }

}
