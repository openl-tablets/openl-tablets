/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial10;


public interface Tutorial_10RulesInterface {
  public static java.lang.String __src = "rules/Tutorial_10.xlsx";


  public org.openl.types.impl.DynamicObject[] getGetCarPrice2010Test();


  public org.openl.types.impl.DynamicObject[] getGetDiscountPercentageTest();


  public org.openl.types.impl.DynamicObject[] getGetPriceForOrderTest();


  public org.openl.tablets.tutorial10.domain.Car[] getTestCars();


  public org.openl.types.impl.DynamicObject[] getGetCarPrice2009Test();


  public org.openl.tablets.tutorial10.domain.Address[] getTestAddresses();


  public org.openl.types.impl.DynamicObject getThis();

  org.openl.rules.testmethod.TestUnitsResults getPriceForOrderTestTestAll();

  org.openl.rules.testmethod.TestUnitsResults getCarPrice2009TestTestAll();

  org.openl.meta.DoubleValue getPriceForOrder(org.openl.tablets.tutorial10.domain.Car car, int numberOfCars, org.openl.tablets.tutorial10.domain.Address billingAddress);

  org.openl.meta.DoubleValue getCarPrice(org.openl.tablets.tutorial10.domain.Car car, org.openl.tablets.tutorial10.domain.Address billingAddress);

  org.openl.rules.testmethod.TestUnitsResults getCarPrice2010TestTestAll();

  org.openl.rules.testmethod.TestUnitsResults getDiscountPercentageTestTestAll();

  org.openl.meta.DoubleValue getDiscountPercentage(org.openl.tablets.tutorial10.domain.Car car, int numberOfCars);

}