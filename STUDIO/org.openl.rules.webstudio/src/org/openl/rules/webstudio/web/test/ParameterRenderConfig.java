package org.openl.rules.webstudio.web.test;

import java.util.Objects;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * This class contains info needed to render parameter in test results page.
 */
public class ParameterRenderConfig {
    private final IOpenClass type;
    private final Object value;
    private final IOpenField keyField;
    private final ParameterDeclarationTreeNode parent;
    private final String fieldNameInParent;
    private final boolean hasExplainLinks;
    private final String requestId;

    private ParameterRenderConfig(Builder builder) {
        this.type = builder.type;
        this.value = builder.value;
        this.keyField = builder.keyField;
        this.parent = builder.parent;
        this.fieldNameInParent = builder.fieldNameInParent;
        this.hasExplainLinks = builder.hasExplainLinks;
        this.requestId = builder.requestId;
    }

    /**
     * Get parameter type
     */
    public IOpenClass getType() {
        return type;
    }

    /**
     * Get parameter value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get the field of a complex object that can be represented as a simple representation of an object. For example
     * this field often is used as a foreign key in Data and Test tables. Key field is shown in parentheses. For example
     * if key field of a Driver type is firstName, the object is displayed as:
     *
     * <pre>
     * Driver (John), Driver (Jill)
     * </pre>
     *
     * @see ComplexParameterTreeNode#getDisplayedValue()
     * @see ComplexParameterTreeNode#ComplexParameterTreeNode(ParameterRenderConfig)
     */
    public IOpenField getKeyField() {
        return keyField;
    }

    /**
     * Get parent object if this parameter is a child of another object.
     */
    public ParameterDeclarationTreeNode getParent() {
        return parent;
    }

    /**
     * Get field name of this parameter in a parent object.
     */
    public String getFieldNameInParent() {
        return fieldNameInParent;
    }

    /**
     * Returns if current object should display explain links?
     *
     * @see SpreadsheetResultTreeNode#getDisplayedValue()
     */
    public boolean isHasExplainLinks() {
        return hasExplainLinks;
    }

    /**
     * Returns unique request id associated with specific test execution. Every test execution stores explanation info
     * in session. We must be able to retrieve and remove that explanation info from session.
     *
     * @see SpreadsheetResultTreeNode#getDisplayedValue()
     * @see org.openl.rules.ui.Explanator#getUniqueId(String, ExplanationNumberValue)
     * @see org.openl.rules.ui.Explanator#remove(String)
     */
    public String getRequestId() {
        return requestId;
    }

    public static class Builder {
        private IOpenClass type;
        private Object value;
        private IOpenField keyField;
        private ParameterDeclarationTreeNode parent;
        private String fieldNameInParent;
        private boolean hasExplainLinks;
        private String requestId;

        /**
         * Create new builder
         *
         * @param type Parameter type. Cannot be null.
         * @param value Parameter value.
         * @see #getType()
         */
        public Builder(IOpenClass type, Object value) {
            this.type = Objects.requireNonNull(type, "type cannot be null");;
            this.value = value;
        }

        /**
         * @see #getKeyField()
         */
        public Builder keyField(IOpenField keyField) {
            this.keyField = keyField;
            return this;
        }

        /**
         * @see #getParent()
         */
        public Builder parent(ParameterDeclarationTreeNode parent) {
            this.parent = parent;
            return this;
        }

        /**
         * @see #getFieldNameInParent()
         */
        public Builder fieldNameInParent(String fieldName) {
            this.fieldNameInParent = fieldName;
            return this;
        }

        /**
         * @see #isHasExplainLinks()
         */
        public Builder hasExplainLinks(boolean hasExplainLinks) {
            this.hasExplainLinks = hasExplainLinks;
            return this;
        }

        /**
         * @see #getRequestId()
         */
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public ParameterRenderConfig build() {
            if (keyField == null) {
                keyField = type.getIndexField();
            }

            return new ParameterRenderConfig(this);
        }
    }
}
