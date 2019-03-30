package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

public class DTColumnsDefinition {

    private Map<String, List<IParameterDeclaration>> localParameters;
    private IOpenMethodHeader header;
    private CompositeMethod compositeMethod;
    private DTColumnsDefinitionType type;

    public DTColumnsDefinition(DTColumnsDefinitionType type,
            Map<String, List<IParameterDeclaration>> localParameters,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        this.localParameters = localParameters;
        this.compositeMethod = compositeMethod;
        this.header = header;
        this.type = type;
    }

    public CompositeMethod getCompositeMethod() {
        return compositeMethod;
    }

    public int getNumberOfTitles() {
        return localParameters.size();
    }

    public List<IParameterDeclaration> getLocalParameters(String title) {
        List<IParameterDeclaration> value = localParameters.get(title);
        if (value != null) {
            return Collections.unmodifiableList(value);
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<IParameterDeclaration> getLocalParameters() {
        return localParameters.values()
            .stream()
            .flatMap(Collection::stream)
            .filter(e -> e != null && e.getName() != null)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public Set<String> getTitles() {
        return Collections.unmodifiableSet(localParameters.keySet());
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    public DTColumnsDefinitionType getType() {
        return type;
    }

    public boolean isCondition() {
        return DTColumnsDefinitionType.CONDITION.equals(type);
    }

    public boolean isAction() {
        return DTColumnsDefinitionType.ACTION.equals(type);
    }

    public boolean isReturn() {
        return DTColumnsDefinitionType.RETURN.equals(type);
    }

}
