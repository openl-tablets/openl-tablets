package org.openl.rules.dt;

import org.openl.meta.DoubleValue;

public interface ILookupTableTest {
    
    DoubleValue getCarPrice(String country, String region, String brand, String model);
    
    DoubleValue getCarPriceMergedHorizontalCond(String country, String region, String brand, String model);
    
    DoubleValue getCarPriceMergedVerticalCondWithRuleCol(String country, String region, String brand, String model);
    
    DoubleValue getCarPriceMergedVerticalCond(String country, String region, String brand, String model);

}
