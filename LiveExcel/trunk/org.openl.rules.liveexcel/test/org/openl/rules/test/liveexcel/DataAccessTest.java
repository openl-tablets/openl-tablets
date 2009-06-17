package org.openl.rules.test.liveexcel;

import java.math.BigDecimal;

import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.Test;
import org.openl.rules.liveexcel.DataPool;
import org.openl.rules.liveexcel.EvaluationContext;
import org.openl.rules.liveexcel.LiveExcelEvaluator;
import org.openl.rules.liveexcel.ServiceModelAPI;
import org.openl.rules.test.liveexcel.formula.PerformanceAndThreadSafetyTest;

import com.exigen.ipb.schemas.rating.VehicleDriverRelationshipType;
import com.exigen.ipb.schemas.rating.VehicleType;
import com.exigen.ipb.schemas.rating.hb.VehicleDriverRelationshipTypeImpl;
import com.exigen.ipb.schemas.rating.hb.VehicleTypeImpl;

import static org.junit.Assert.*;

public class DataAccessTest {
    @Test
    public void test() {
        HSSFWorkbook workbook = PerformanceAndThreadSafetyTest.getHSSFWorkbook("./test/resources/DataAccessTest.xls");
        EvaluationContext context = new EvaluationContext(new DataPool(), new ServiceModelAPI("SimpleExample"));
        LiveExcelEvaluator evaluator = new LiveExcelEvaluator(workbook, context);
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
