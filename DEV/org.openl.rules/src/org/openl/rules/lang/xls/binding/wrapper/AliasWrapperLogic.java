package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractAlgorithmSubroutineMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractAlgorithmWrapper;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractColumnMatchWrapper;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractDecisionTableWrapper;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractSpreadsheetWrapper;
import org.openl.rules.lang.xls.binding.wrapper.base.AbstractTableMethodWrapper;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.types.IOpenMethod;

public final class AliasWrapperLogic {
    private AliasWrapperLogic() {
    }

    public static IOpenMethod wrapOpenMethod(final IOpenMethod openMethod, final String aliasMethodName) {
        if (openMethod instanceof Algorithm) {
            return new AbstractAlgorithmWrapper((Algorithm) openMethod) {
                @Override
                public String getName() {
                    return aliasMethodName;
                }
            };
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return new AbstractAlgorithmSubroutineMethodWrapper((AlgorithmSubroutineMethod) openMethod) {
                @Override
                public String getName() {
                    return aliasMethodName;
                }
            };
        }
        if (openMethod instanceof DecisionTable) {
            return new AbstractDecisionTableWrapper((DecisionTable) openMethod) {
                @Override
                public String getName() {
                    return aliasMethodName;
                }
            };
        }
        if (openMethod instanceof ColumnMatch) {
            return new AbstractColumnMatchWrapper((ColumnMatch) openMethod) {
                @Override
                public String getName() {
                    return aliasMethodName;
                }
            };
        }
        if (openMethod instanceof Spreadsheet) {
            return new AbstractSpreadsheetWrapper((Spreadsheet) openMethod) {
                @Override
                public String getName() {
                    return aliasMethodName;
                }
            };
        }
        if (openMethod instanceof TableMethod) {
            return new AbstractTableMethodWrapper((TableMethod) openMethod) {
                @Override
                public String getName() {
                    return aliasMethodName;
                }
            };
        }

        throw new IllegalStateException(String.format("Unsupported method type '%s' for service method wrapping",
            openMethod.getClass().getTypeName()));
    }
}
