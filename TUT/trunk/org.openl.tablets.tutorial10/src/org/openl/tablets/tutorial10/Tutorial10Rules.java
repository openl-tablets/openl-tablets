package org.openl.tablets.tutorial10;

import org.openl.meta.DoubleValue;

public interface Tutorial10Rules {
    DoubleValue getPriceForOrder(Car car, int numberOfCars, Address address);
}
