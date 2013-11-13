package org.openl.rules.tutorial4;

import org.openl.generated.beans.publisher.test.Driver;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;

public interface Tutorial4WithVariations {
    String driverAgeType(Driver driver);

    VariationsResult<String> driverAgeType(Driver driver, VariationsPack variations);
}
