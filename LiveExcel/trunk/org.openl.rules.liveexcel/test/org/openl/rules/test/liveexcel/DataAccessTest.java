package org.openl.rules.test.liveexcel;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.Test;
import org.openl.rules.liveexcel.DataPool;
import org.openl.rules.liveexcel.EvaluationContext;
import org.openl.rules.liveexcel.LiveExcelEvaluator;
import org.openl.rules.liveexcel.ServiceModelAPI;
import org.openl.rules.test.liveexcel.formula.PerformanceAndThreadSafetyTest;
import static org.junit.Assert.*;

public class DataAccessTest {
    public class Wife {
        private int AGE;

        public Wife(int age) {
            AGE = age;
        }

        public int getAGE() {
            return AGE;
        }
    }

    public class Man {
        private int AGE;
        private Wife WIFE;

        public Man(int age, Wife wife) {
            AGE = age;
            WIFE = wife;
        }

        public int getAGE() {
            return AGE;
        }

        public Wife getWIFE() {
            return WIFE;
        }
    }

    @Test
    public void test() {
        HSSFWorkbook workbook = PerformanceAndThreadSafetyTest.getHSSFWorkbook("./test/resources/DataAccessTest.xls");
        EvaluationContext context = new EvaluationContext(new DataPool(), new ServiceModelAPI() {
            public List<String> getAllServiceModelUDFs() {
                List<String> udfs = new ArrayList<String>();
                udfs.add("AGE");
                udfs.add("WIFE");
                return udfs;
            }
        });
        LiveExcelEvaluator evaluator = new LiveExcelEvaluator(workbook, context);
        assertTrue(5.0 == ((NumberEval) evaluator.evaluateServiceModelUDF("func1",
                new Object[] { new Man(30, new Wife(25)) })).getNumberValue());
        assertTrue(23.0 == ((NumberEval) evaluator.evaluateServiceModelUDF("func1",
                new Object[] { new Man(41, new Wife(18)) })).getNumberValue());
    }
}
