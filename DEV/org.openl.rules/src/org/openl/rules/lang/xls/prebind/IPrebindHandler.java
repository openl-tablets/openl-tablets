package org.openl.rules.lang.xls.prebind;

import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

/**
 * Serves to postprocess fields and methods after prebind. All methods and fields before adding to ModuleOpenClass
 * should be wrapped to some implementations that will be invokable(Because original methods and fields are only
 * prebinded).
 *
 * @author PUdalau
 */
public interface IPrebindHandler {
    IOpenMethod processPrebindMethod(IOpenMethod method);

    IOpenField processPrebindField(IOpenField field);
}
