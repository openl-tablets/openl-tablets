package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parent model for executable tables
 *
 * @author Vladyslav Pikus
 */
public abstract class ExecutableView extends TableView implements EditableTableView {

    @Schema(description = "Return type of the executable table")
    public final String returnType;

    @Schema(description = "List of input arguments/parameters for the executable table")
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
