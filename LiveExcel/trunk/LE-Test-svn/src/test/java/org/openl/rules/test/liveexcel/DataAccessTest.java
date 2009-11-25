package org.openl.rules.test.liveexcel;

import java.io.FileInputStream;
import java.math.BigDecimal;

import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.junit.Test;
import org.openl.rules.liveexcel.LiveExcelEvaluator;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;

import com.exigen.ipb.schemas.rating.VehicleDriverRelationshipType;
import com.exigen.ipb.schemas.rating.VehicleType;
import com.exigen.ipb.schemas.rating.hb.VehicleDriverRelationshipTypeImpl;
import com.exigen.ipb.schemas.rating.hb.VehicleTypeImpl;

import static org.junit.Assert.*;

public class DataAccessTest {
    @Test
    public void test() throws Exception{
        LiveExcelWorkbook workbook = LiveExcelWorkbookFactory.create(new FileInputStream("./test/resources/DataAccessTest.xls"), "SimpleExample");
        LiveExcelEvaluator evaluator = new LiveExcelEvaluator(workbook, workbook.getEvaluationContext());
        VehicleType vehicleType = new VehicleTypeImpl();
        VehicleDriverRelationshipType principalDriver = new VehicleDriverRelationshipTypeImpl();
        VehicleDriverRelationshipType principalDriver2 = new VehicleDriverRelationshipTypeImpl();
        vehicleType.setPrincipalDriver(principalDriver);
        principalDriver.setPercentageOfUse(new BigDecimal(23));
        principalDriver2.setPercentageOfUse(new BigDecimal(0));
        assertTrue(23.0 == ((NumberEval) evaluator.evaluateServiceModelUDF("func1", new Object[]{vehicleType, principalDriver2})).getNumberValue());
        principalDriver2.setPercentageOfUse(new BigDecimal(24));
        assertTrue(1 == ((NumberEval) evaluator.evaluateServiceModelUDF("func1", new Object[]{vehicleType, principalDriver2})).getNumberValue());
    }
}
