package org.openl.rules.table.properties.def;

import lombok.Getter;
import lombok.Setter;

import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenClass;

public class TablePropertyDefinition implements Comparable<TablePropertyDefinition> {

    @Getter
    @Setter
    private String displayName;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private boolean primaryKey;
    @Getter
    @Setter
    private IOpenClass type;
    @Getter
    @Setter
    private String group;
    @Getter
    @Setter
    private boolean system;
    @Getter
    @Setter
    private String systemValueDescriptor;
    @Getter
    @Setter
    private SystemValuePolicy systemValuePolicy;
    @Getter
    @Setter
    private boolean dimensional;
    @Getter
    @Setter
    private String securityFilter;
    @Getter
    @Setter
    private XlsNodeTypes[] tableType;
    @Getter
    @Setter
    private String defaultValue;
    @Getter
    @Setter
    private Constraints constraints;
    @Getter
    @Setter
    private String format;
    @Getter
    @Setter
    private InheritanceLevel[] inheritanceLevel;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private MatchingExpression expression;
    @Getter
    @Setter
    private Severity errorSeverity;
    @Getter
    @Setter
    private String deprecation;

    public enum SystemValuePolicy {
        IF_BLANK_ONLY,
        ON_EACH_EDIT
    }

    @Override
    public int compareTo(TablePropertyDefinition to) {
        return displayName.compareTo(to.getDisplayName());
    }

}
