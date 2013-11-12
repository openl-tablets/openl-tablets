package org.openl.rules.domaintype;

import org.openl.meta.StringValue;

/**
 *
 * @author snshor
 *
 */

public class DomainType {

    private StringValue domainName;
    private StringValue baseType;
    private StringValue varName;
    private StringValue useVarPattern;
    private StringValue description;
    private StringValue displayName;
    private StringValue domainExpression;

    public StringValue getBaseType() {
        return baseType;
    }

    public StringValue getDescription() {
        return description;
    }

    public StringValue getDisplayName() {
        return displayName;
    }

    public StringValue getDomainExpression() {
        return domainExpression;
    }

    public StringValue getDomainName() {
        return domainName;
    }

    public StringValue getUseVarPattern() {
        return useVarPattern;
    }

    public StringValue getVarName() {
        return varName;
    }

    public void setBaseType(StringValue baseType) {
        this.baseType = baseType;
    }

    public void setDescription(StringValue description) {
        this.description = description;
    }

    public void setDisplayName(StringValue displayName) {
        this.displayName = displayName;
    }

    public void setDomainExpression(StringValue domainExpression) {
        this.domainExpression = domainExpression;
    }

    public void setDomainName(StringValue domainName) {
        this.domainName = domainName;
    }

    public void setUseVarPattern(StringValue useVarPattern) {
        this.useVarPattern = useVarPattern;
    }

    public void setVarName(StringValue varName) {
        this.varName = varName;
    }
}
