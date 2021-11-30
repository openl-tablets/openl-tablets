package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;

public class LookupTableTest {

    public interface ILookupTableTest {

        Double getCarPrice(String country, String region, String brand, String model);

        Double getCarPriceMergedHorizontalCond(String country, String region, String brand, String model);

        Double getCarPriceMergedVerticalCondWithRuleCol(String country, String region, String brand, String model);

        Double getCarPriceMergedVerticalCond(String country, String region, String brand, String model);

    }

    private static final String SRC = "test/rules/dt/lookup/LookUpTableTest.xls";

    private ILookupTableTest instance;

    @Before
    public void initEngine() {
        RulesEngineFactory<ILookupTableTest> engineFactory = new RulesEngineFactory<>(SRC, ILookupTableTest.class);

        instance = engineFactory.newEngineInstance();
    }

    @Test
    public void testNotMergerdLookupTable() {
        Double result = instance.getCarPrice("Belarus,UK", "Minsk", "Porche", "911 Carrera 4S");
        assertEquals(93200, result.intValue());
    }

    @Test
    public void testMergedHorizontalCond() {
        Double result = instance.getCarPriceMergedHorizontalCond("Belarus", "Minsk", "Porche", "911 Targa 4");
        assertEquals(90400, result.intValue());
    }

    @Test
    public void testMergedVerticalCondWithRuleCol() {
        Double result = instance
            .getCarPriceMergedVerticalCondWithRuleCol("Belarus", "Minsk", "Porche", "911 Targa 4");
        assertEquals(90401, result.intValue());
        result = instance.getCarPriceMergedVerticalCondWithRuleCol("Belarus", "Vitebsk", "Porche", "911 Targa 4");
        assertEquals(90402, result.intValue());

        result = instance.getCarPriceMergedVerticalCondWithRuleCol("GreatBritain",
            "Wales",
            "Audi",
            "2009 Audi R8 4.2 quattro 6-Speed Manual");
        assertEquals(112501, result.intValue());

    }

    @Test
    public void testMergedVerticalCond() {
        Double result = instance.getCarPriceMergedVerticalCond("Belarus", "Minsk", "Porche", "911 Targa 4");
        assertEquals(90401, result.intValue());
        result = instance.getCarPriceMergedVerticalCond("Belarus", "Vitebsk", "Porche", "911 Targa 4");
        assertEquals(90402, result.intValue());

        result = instance
            .getCarPriceMergedVerticalCond("GreatBritain", "Wales", "Audi", "2009 Audi R8 4.2 quattro 6-Speed Manual");
        assertEquals(112502, result.intValue());
    }

}
