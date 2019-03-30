package org.openl.rules.table.properties.def;

import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenClass;

public class TablePropertyDefinition implements Comparable<TablePropertyDefinition> {

    private String displayName;
    private String name;
    private boolean primaryKey;
    private IOpenClass type;
    private String group;
    private boolean system;
    private String systemValueDescriptor;
    private SystemValuePolicy systemValuePolicy;
    private boolean dimensional;
    private String securityFilter;
    private XlsNodeTypes[] tableType;
    private String defaultValue;
    private Constraints constraints;
    private String format;
    private InheritanceLevel[] inheritanceLevel;
    private String description;
    private MatchingExpression expression;
    private Severity errorSeverity;
    private String deprecation;

    public enum SystemValuePolicy {
        IF_BLANK_ONLY,
        ON_EACH_EDIT
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public IOpenClass getType() {
        return type;
    }

    public void setType(IOpenClass type) {
        this.type = type;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSecurityFilter() {
        return securityFilter;
    }

    public void setSecurityFilter(String securityFilter) {
        this.securityFilter = securityFilter;
    }

    public XlsNodeTypes[] getTableType() {
        return tableType;
    }

    public void setTableType(XlsNodeTypes[] tableType) {
        this.tableType = tableType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDimensional() {
        return dimensional;
    }

    public void setDimensional(boolean dimensional) {
        this.dimensional = dimensional;
    }

    public MatchingExpression getExpression() {
        return expression;
    }

    public void setExpression(MatchingExpression expression) {
        this.expression = expression;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystemValueDescriptor(String systemValueDescriptor) {
        this.systemValueDescriptor = systemValueDescriptor;
    }

    public String getSystemValueDescriptor() {
        return systemValueDescriptor;
    }

    public void setSystemValuePolicy(SystemValuePolicy systemValuePolicy) {
        this.systemValuePolicy = systemValuePolicy;
    }

    public SystemValuePolicy getSystemValuePolicy() {
        return systemValuePolicy;
    }

    public void setInheritanceLevel(InheritanceLevel[] inheritanceLevel) {
        this.inheritanceLevel = inheritanceLevel;
    }

    public InheritanceLevel[] getInheritanceLevel() {
        return inheritanceLevel;
    }

    public Severity getErrorSeverity() {
        return errorSeverity;
    }

    public void setErrorSeverity(Severity errorSeverity) {
        this.errorSeverity = errorSeverity;
    }

    public String getDeprecation() {
        return deprecation;
    }

    public void setDeprecation(String deprecation) {
        this.deprecation = deprecation;
    }

    @Override
    public int compareTo(TablePropertyDefinition to) {
        return displayName.compareTo(to.getDisplayName());
    }

}
