package org.openl.rules.tutorial4;

import org.openl.generated.beans.Driver;
import org.openl.rules.project.instantiation.variation.VariationsPack;
import org.openl.rules.project.instantiation.variation.VariationsResult;

public interface Tutorial4WithVariations {
    String driverAgeType(Driver driver);

    VariationsResult<String> driverAgeType(Driver driver, VariationsPack variations);
}
