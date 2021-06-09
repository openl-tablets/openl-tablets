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

    private static class AliasAlgorithmWrapper extends AbstractAlgorithmWrapper {
        private final String aliasMethodName;

        public AliasAlgorithmWrapper(Algorithm delegate, String aliasMethodName) {
            super(delegate);
            this.aliasMethodName = aliasMethodName;
        }

        @Override
        public String getName() {
            return aliasMethodName;
        }

        @Override
        public boolean isAlias() {
            return true;
        }
    }

    private static class AliasAlgorithmSubroutineMethodWrapper extends AbstractAlgorithmSubroutineMethodWrapper {
        private final String aliasMethodName;

        public AliasAlgorithmSubroutineMethodWrapper(AlgorithmSubroutineMethod delegate, String aliasMethodName) {
            super(delegate);
            this.aliasMethodName = aliasMethodName;
        }

        @Override
        public String getName() {
            return aliasMethodName;
        }

        @Override
        public boolean isAlias() {
            return true;
        }

    }

    private static class AliasDecisionTableWrapper extends AbstractDecisionTableWrapper {
        private final String aliasMethodName;

        public AliasDecisionTableWrapper(DecisionTable delegate, String aliasMethodName) {
            super(delegate);
            this.aliasMethodName = aliasMethodName;
        }

        @Override
        public String getName() {
            return aliasMethodName;
        }

        @Override
        public boolean isAlias() {
            return true;
        }

    }

    private static class AliasColumnMatchWrapper extends AbstractColumnMatchWrapper {
        private final String aliasMethodName;

        public AliasColumnMatchWrapper(ColumnMatch delegate, String aliasMethodName) {
            super(delegate);
            this.aliasMethodName = aliasMethodName;
        }

        @Override
        public String getName() {
            return aliasMethodName;
        }

        @Override
        public boolean isAlias() {
            return true;
        }

    }

    private static class AliasSpreadsheetWrapper extends AbstractSpreadsheetWrapper {
        private final String aliasMethodName;

        public AliasSpreadsheetWrapper(Spreadsheet delegate, String aliasMethodName) {
            super(delegate);
            this.aliasMethodName = aliasMethodName;
        }

        @Override
        public String getName() {
            return aliasMethodName;
        }

        @Override
        public boolean isAlias() {
            return true;
        }

    }

    private static class AliasTableMethodWrapper extends AbstractTableMethodWrapper {
        private final String aliasMethodName;

        public AliasTableMethodWrapper(TableMethod delegate, String aliasMethodName) {
            super(delegate);
            this.aliasMethodName = aliasMethodName;
        }

        @Override
        public String getName() {
            return aliasMethodName;
        }

        @Override
        public boolean isAlias() {
            return true;
        }

    }

    public static IOpenMethod wrapOpenMethod(final IOpenMethod openMethod, final String aliasMethodName) {
        if (openMethod instanceof Algorithm) {
            return new AliasAlgorithmWrapper((Algorithm) openMethod, aliasMethodName);
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return new AliasAlgorithmSubroutineMethodWrapper((AlgorithmSubroutineMethod) openMethod, aliasMethodName);
        }
        if (openMethod instanceof DecisionTable) {
            return new AliasDecisionTableWrapper((DecisionTable) openMethod, aliasMethodName);
        }
        if (openMethod instanceof ColumnMatch) {
            return new AliasColumnMatchWrapper((ColumnMatch) openMethod, aliasMethodName);
        }
        if (openMethod instanceof Spreadsheet) {
            return new AliasSpreadsheetWrapper((Spreadsheet) openMethod, aliasMethodName);
        }
        if (openMethod instanceof TableMethod) {
            return new AliasTableMethodWrapper((TableMethod) openMethod, aliasMethodName);
        }
        throw new IllegalStateException(
            String.format("Unsupported method type '%s' for method wrapping with alias functionality",
                openMethod.getClass().getTypeName()));
    }
}
