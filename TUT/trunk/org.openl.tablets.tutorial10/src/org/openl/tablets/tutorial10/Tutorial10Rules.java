package org.openl.tablets.tutorial10;

import org.openl.meta.DoubleValue;
import org.openl.tablets.tutorial10.domain.Address;
import org.openl.tablets.tutorial10.domain.Car;

public interface Tutorial10Rules {
    DoubleValue getPriceForOrder(Car car, int numberOfCars, Address address);
}
