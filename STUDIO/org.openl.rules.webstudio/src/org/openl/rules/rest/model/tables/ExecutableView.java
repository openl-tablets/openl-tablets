package org.openl.rules.rest.model.tables;

import java.util.Collection;

/**
 * Parent model for executable tables
 *
 * @author Vladyslav Pikus
 */
public abstract class ExecutableView extends TableView implements EditableTableView {

    public final String returnType;

    public final Collection<ArgumentView> args;

    protected ExecutableView(Builder<?> builder) {
        super(builder);
        this.returnType = builder.returnType;
        this.args = builder.args;
    }

    @Override
    public String getTableType() {
        return tableType;
    }

    public static abstract class Builder<T extends ExecutableView.Builder<T>> extends TableView.Builder<T> {
        private String returnType;
        private Collection<ArgumentView> args;

        protected Builder() {
        }

        public T returnType(String returnType) {
            this.returnType = returnType;
            return self();
        }

        public T args(Collection<ArgumentView> args) {
            this.args = args;
            return self();
        }

    }

}
