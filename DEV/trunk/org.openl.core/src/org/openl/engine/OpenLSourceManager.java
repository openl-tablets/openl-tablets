package org.openl.engine;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceType;
import org.openl.syntax.code.IDependency;
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
        return processSource(source, sourceType, null, false, null);
    }

    /**
     * Parses and binds source.
     * 
     * @param source source
     * @param sourceType type of source
     * @return processed code descriptor
     */
    public ProcessedCode processSource(IOpenSourceCodeModule source, SourceType sourceType, IDependencyManager dependencyManager) {
        return processSource(source, sourceType, null, false, dependencyManager);
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
                                       boolean ignoreErrors, 
                                       IDependencyManager dependencyManager) {

        IParsedCode parsedCode = parseManager.parseSource(source, sourceType);
        SyntaxNodeException[] parsingErrors = parsedCode.getErrors();

        if (!ignoreErrors && parsingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", parsingErrors);
        }

        // compile source dependencies        
        if (SourceType.MODULE.equals(sourceType)) {

            IDependency[] dependencies = parsedCode.getDependencies();
            Set<CompiledOpenClass> compiledDependencies = new LinkedHashSet<CompiledOpenClass>();

            if (dependencies != null && dependencies.length > 0) {
                if (dependencyManager != null) {
               
                    for (int i = 0; i < dependencies.length; i++) {
                        try {
                            CompiledDependency loadedDependency = dependencyManager.loadDependency(dependencies[i]);
                            OpenLBundleClassLoader currentClassLoader = (OpenLBundleClassLoader) Thread.currentThread().getContextClassLoader();
                            currentClassLoader.addClassLoader(loadedDependency.getClassLoader());
                            compiledDependencies.add(loadedDependency.getCompiledOpenClass());
                        } catch (Exception e) {
                            throw new OpenLRuntimeException(e);
                        }
                    }
                } else {
                    throw new OpenLRuntimeException("Dependency manager is not defined");
                }
            }

            parsedCode.setCompiledDependencies(compiledDependencies);
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

}
