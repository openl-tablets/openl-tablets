package org.openl.rules.ruleservice.publish.lazy.wrapper;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.property.PropertiesOpenField;
import org.openl.rules.ruleservice.publish.lazy.LazyField;
import org.openl.rules.ruleservice.publish.lazy.LazyMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

public final class LazyWrapperLogic {
    private LazyWrapperLogic() {
    }

    public static IOpenMethod wrapMethod(LazyMethod lazyMethod, IOpenMethod openMethod) {
        if (openMethod instanceof TestSuiteMethod) {
            return openMethod;
        }
        if (openMethod instanceof OverloadedMethodsDispatcherTable) {
            return new OverloadedMethodsDispatcherTableLazyWrapper(lazyMethod,
                (OverloadedMethodsDispatcherTable) openMethod);
        }
        if (openMethod instanceof MatchingOpenMethodDispatcher) {
            return new MatchingOpenMethodDispatcherLazyWrapper(lazyMethod, (MatchingOpenMethodDispatcher) openMethod);
        }
        if (openMethod instanceof Algorithm) {
            return new AlgorithmLazyWrapper(lazyMethod, (Algorithm) openMethod);
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return new AlgorithmSubroutineMethodLazyWrapper(lazyMethod, (AlgorithmSubroutineMethod) openMethod);
        }
        if (openMethod instanceof DecisionTable) {
            return new DecisionTable2LazyWrapper(lazyMethod, (DecisionTable) openMethod);
        }
        if (openMethod instanceof ColumnMatch) {
            return new ColumnMatchLazyWrapper(lazyMethod, (ColumnMatch) openMethod);
        }
        if (openMethod instanceof Spreadsheet) {
            return new SpreadsheetLazyWrapper(lazyMethod, (Spreadsheet) openMethod);
        }
        if (openMethod instanceof TableMethod) {
            return new TableMethodLazyWrapper(lazyMethod, (TableMethod) openMethod);
        }
        throw new IllegalStateException(
            String.format("Unsupported method type '%s' for lazy enhancing", openMethod.getClass().getTypeName()));
    }

    public static IOpenField wrapField(LazyField lazyField, IOpenField field) {
        if (field instanceof ConstantOpenField) {
            return field;
        }
        if (field instanceof PropertiesOpenField) {
            return new PropertiesOpenFieldLazyWrapper(lazyField, (PropertiesOpenField) field);
        }
        if (field instanceof DataOpenField) {
            return new DataOpenFieldLazyWrapper(lazyField, (DataOpenField) field);
        }
        throw new IllegalStateException(
            String.format("Unsupported field type '%s' for lazy enhancing", field.getClass().getTypeName()));
    }
}
