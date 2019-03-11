package org.openl.rules.calculation.result.convertor2.sample;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.openl.rules.calculation.result.convertor2.sample.result.ResultConvertor;
import org.openl.rules.calculation.result.convertor2.sample.result.ResultConvertorWithBlackList;
import org.openl.rules.calculation.result.convertor2.sample.result.ResultConvertorWithWhiteList;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.impl.URLSourceCodeModule;

public class ConvertorUsesTitlesTest {
    public interface ITestCalc {
        SpreadsheetResult calc();
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc0-1.xls");
        RulesEngineFactory<ITestCalc> engineFactory = new RulesEngineFactory<>(URLSourceCodeModule.toUrl(xlsFile), ITestCalc.class);

        ITestCalc test = engineFactory.newEngineInstance();
        SpreadsheetResult result = test.calc();
        assertEquals(2, result.getHeight());
        assertEquals(3, result.getWidth());
        
        ResultConvertor resultConvertor = new ResultConvertor();
        CompoundStep compoundStep = resultConvertor.process(result);
        List<CalculationStep> steps = compoundStep.getSteps();
        assertEquals(2, steps.size());
        assertEquals("Row1", steps.get(0).getStepName());
        assertEquals("Row2", steps.get(1).getStepName());
    }
    
    @Test
    public void whiteListTest() {
        File xlsFile = new File("test/rules/calc0-1.xls");
        RulesEngineFactory<ITestCalc> engineFactory = new RulesEngineFactory<>(URLSourceCodeModule.toUrl(xlsFile), ITestCalc.class);

        ITestCalc test = engineFactory.newEngineInstance();
        SpreadsheetResult result = test.calc();
        assertEquals(2, result.getHeight());
        assertEquals(3, result.getWidth());
        
        ResultConvertorWithWhiteList resultConvertor = new ResultConvertorWithWhiteList();
        CompoundStep compoundStep = resultConvertor.process(result);
        List<CalculationStep> steps = compoundStep.getSteps();
        assertEquals(1, steps.size());
        assertEquals("Row1", steps.get(0).getStepName());
    }
    
    @Test
    public void blackListTest() {
        File xlsFile = new File("test/rules/calc0-1.xls");
        RulesEngineFactory<ITestCalc> engineFactory = new RulesEngineFactory<>(URLSourceCodeModule.toUrl(xlsFile), ITestCalc.class);

        ITestCalc test = engineFactory.newEngineInstance();
        SpreadsheetResult result = test.calc();
        assertEquals(2, result.getHeight());
        assertEquals(3, result.getWidth());
        
        ResultConvertorWithBlackList resultConvertor = new ResultConvertorWithBlackList();
        CompoundStep compoundStep = resultConvertor.process(result);
        List<CalculationStep> steps = compoundStep.getSteps();
        assertEquals(1, steps.size());
        assertEquals("Row1", steps.get(0).getStepName());
    }
}
