package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

public class DTColumnsDefinition {

    @Getter
    private final String tableName;
    @Getter
    private final String expression;
    private final Map<String, List<IParameterDeclaration>> parameters;
    @Getter
    private final IOpenMethodHeader header;
    @Setter
    private CompositeMethod compositeMethod;
    @Getter
    private final DTColumnsDefinitionType type;
    @Getter
    private final String uri;
    @Setter
    private Set<String> externalParameters;
    @Setter
    private Runnable compositeMethodInitializer;
    @Getter
    private final List<ExpressionIdentifier> identifiers;

    public DTColumnsDefinition(DTColumnsDefinitionType type,
                               String tableName,
                               IOpenMethodHeader header,
                               String expression,
                               List<ExpressionIdentifier> identifiers,
                               Map<String, List<IParameterDeclaration>> parameters,
                               TableSyntaxNode tableSyntaxNode) {
        this.tableName = tableName;
        this.header = Objects.requireNonNull(header, "header cannot be null");
        this.identifiers = Collections
                .unmodifiableList(Objects.requireNonNull(identifiers, "identifiers cannot be null"));
        this.expression = Objects.requireNonNull(expression, "expression cannot be null");
        this.parameters = Objects.requireNonNull(parameters, "parameters cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.uri = tableSyntaxNode.getUri();
    }

    public CompositeMethod getCompositeMethod() {
        if (compositeMethod == null) {
            compositeMethodInitializer.run();
        }
        return compositeMethod;
    }

    public Set<String> getExternalParameters() {
        return Objects.requireNonNullElse(externalParameters, Set.of());
    }

    public int getNumberOfTitles() {
        return parameters.size();
    }

    public List<IParameterDeclaration> getParameters() {
        return parameters.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<IParameterDeclaration> getParameters(String title) {
        List<IParameterDeclaration> value = parameters.get(title);
        if (value != null) {
            return Collections.unmodifiableList(value);
        } else {
            return List.of();
        }
    }

    public Set<String> getTitles() {
        return Collections.unmodifiableSet(parameters.keySet());
    }

    public boolean isCondition() {
        return DTColumnsDefinitionType.CONDITION == type;
    }

    public boolean isAction() {
        return DTColumnsDefinitionType.ACTION == type;
    }

    public boolean isReturn() {
        return DTColumnsDefinitionType.RETURN == type;
    }
}
