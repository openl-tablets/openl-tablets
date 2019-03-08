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
    
    private Map<String, List<IParameterDeclaration>> parameterDeclarations;
    private IOpenMethodHeader header;
    private CompositeMethod compositeMethod;
    private DTColumnsDefinitionType type;
    
    public DTColumnsDefinition(DTColumnsDefinitionType type, 
            Map<String, List<IParameterDeclaration>> parameterDeclarations,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        this.parameterDeclarations = parameterDeclarations;
        this.compositeMethod = compositeMethod;
        this.header = header;
        this.type = type;
    }
    
    public CompositeMethod getCompositeMethod() {
        return compositeMethod;
    }

    public int getNumberOfTitles() {
        return parameterDeclarations.size();
    }

    public List<IParameterDeclaration> getParameterDeclarations(String title) {
        List<IParameterDeclaration> value = parameterDeclarations.get(title);
        if (value != null) {
            return Collections.unmodifiableList(value);
        } else {
            return Collections.emptyList();
        }
    }
    
    public Collection<IParameterDeclaration> getParameterDeclarations() {
        return parameterDeclarations.values()
            .stream()
            .flatMap(e -> e.stream())
            .filter(e -> e != null)
            .collect(Collectors.toCollection(ArrayList::new));
    }
    
    public Set<String> getTitles() {
        return Collections.unmodifiableSet(parameterDeclarations.keySet());
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }
    
    public DTColumnsDefinitionType getType() {
        return type;
    }
}
