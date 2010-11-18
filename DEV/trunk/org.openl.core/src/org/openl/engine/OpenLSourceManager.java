package org.openl.engine;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceType;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * Class that defines OpenL engine manager implementation for source processing operations.
 * 
 */
public class OpenLSourceManager extends OpenLHolder {

    private OpenLParseManager parseManager;
    private OpenLBindManager bindManager;

    /**
     * Create new instance of OpenL engine manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenLSourceManager(OpenL openl) {

        super(openl);

        bindManager = new OpenLBindManager(openl);
        parseManager = new OpenLParseManager(openl);

    }

    /**
     * Parses and binds source.
     * 
     * @param source source
     * @param sourceType type of source
     * @return processed code descriptor
     */
    public ProcessedCode processSource(IOpenSourceCodeModule source, SourceType sourceType) {

        return processSource(source, sourceType, null, false);
    }

    /**
     * Parses and binds source.
     * 
     * @param source source
     * @param sourceType type of source
     * @param bindingContextDelegator binding context
     * @param ignoreErrors define a flag that indicates to suppress errors or break source processing when an error has
     *            occurred
     * @return processed code descriptor
     */
    public ProcessedCode processSource(IOpenSourceCodeModule source,
                                       SourceType sourceType,
                                       IBindingContextDelegator bindingContextDelegator,
                                       boolean ignoreErrors) {

        IParsedCode parsedCode = parseManager.parseSource(source, sourceType);

        SyntaxNodeException[] parsingErrors = parsedCode.getErrors();

        if (!ignoreErrors && parsingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", parsingErrors);
        }
        
        // compile source dependencies        
        if (SourceType.MODULE.equals(sourceType) ) {
            Set<IOpenSourceCodeModule> dependentSources = parsedCode.getDependentSources();
            if (!dependentSources.isEmpty()) {
                Set<CompiledOpenClass> compiledDependencies = compileDependencies(dependentSources);
                parsedCode.setCompiledDependencies(compiledDependencies);
            }           
        }
        
        Map<String, Object> externalParams = source.getParams();

        if (externalParams != null) {
            parsedCode.setExternalParams(externalParams);
        }        
        
        IBoundCode boundCode = bindManager.bindCode(bindingContextDelegator, parsedCode);

        SyntaxNodeException[] bindingErrors = boundCode.getErrors();

        if (!ignoreErrors && bindingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Binding Error:", bindingErrors);
        }

        ProcessedCode processedCode = new ProcessedCode();
        processedCode.setParsedCode(parsedCode);
        processedCode.setBoundCode(boundCode);
        
        return processedCode;
    }

    private Set<CompiledOpenClass> compileDependencies(Set<IOpenSourceCodeModule> dependentSources) {
        Set<CompiledOpenClass> compiledDependencies = new HashSet<CompiledOpenClass>();
        for (IOpenSourceCodeModule dependentSource : dependentSources) {
            CompiledOpenClass compiledDependency = OpenLManager.compileModuleWithErrors(getOpenL(), dependentSource);
            if (compiledDependency != null) {
                compiledDependencies.add(compiledDependency);
            } else {
                // error during compilation, throw smth
            }
        }
        return compiledDependencies;
    }

}
