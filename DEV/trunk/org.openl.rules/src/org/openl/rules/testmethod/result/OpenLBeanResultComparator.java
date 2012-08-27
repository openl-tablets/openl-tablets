package org.openl.rules.testmethod.result;

import java.util.List;

import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

/**
 * Similar to {@link BeanResultComparator} but uses OpenL core API.
 * 
 * @author PUdalau
 * 
 */
public class OpenLBeanResultComparator implements TestResultComparator {
    private List<IOpenField> fieldsToCompare;

    public OpenLBeanResultComparator(List<IOpenField> fields) {
        this.fieldsToCompare = fields;
    }

    public List<IOpenField> getFieldsToCompare() {
        return fieldsToCompare;
    }

    public boolean compareResult(Object actualResult, Object expectedResult) {
        if (actualResult == null || expectedResult == null) {
            return actualResult == expectedResult;
        } else {
            IRuntimeEnv env = new SimpleVM().getRuntimeEnv();

            for (IOpenField fieldToCompare : fieldsToCompare) {
                Object actualFieldValue = fieldToCompare.get(actualResult, env);
                Object expectedFieldValue = fieldToCompare.get(expectedResult, env);

                TestResultComparator comparator = TestResultComparatorFactory.getComparator(actualFieldValue,
                    expectedFieldValue);
                boolean compare = comparator.compareResult(actualFieldValue, expectedFieldValue);

                if (!compare) {
                    return false;
                }
            }
            return true;
        }

    }
}
