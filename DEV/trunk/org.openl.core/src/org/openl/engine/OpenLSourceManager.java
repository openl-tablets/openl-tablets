package org.openl.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * Class that defines OpenL engine manager implementation for source processing
 * operations.
 * 
 */
public class OpenLSourceManager extends OpenLHolder {

    private static final String EXTERNAL_DEPENDENCIES_KEY = "external-dependencies";
    
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
    public ProcessedCode processSource(IOpenSourceCodeModule source, SourceType sourceType,
        IDependencyManager dependencyManager) {
        return processSource(source, sourceType, null, false, dependencyManager);
    }

    /**
     * Parses and binds source.
     * 
     * @param source source
     * @param sourceType type of source
     * @param bindingContextDelegator binding context
     * @param ignoreErrors define a flag that indicates to suppress errors or
     *            break source processing when an error has occurred
     * @return processed code descriptor
     */
    public ProcessedCode processSource(IOpenSourceCodeModule source, SourceType sourceType,
        IBindingContextDelegator bindingContextDelegator, boolean ignoreErrors, IDependencyManager dependencyManager) {

        IParsedCode parsedCode = parseManager.parseSource(source, sourceType);
        SyntaxNodeException[] parsingErrors = parsedCode.getErrors();

        if (!ignoreErrors && parsingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", parsingErrors);
        }

        // compile source dependencies
        if (SourceType.MODULE.equals(sourceType)) {

            Set<CompiledOpenClass> compiledDependencies = new LinkedHashSet<CompiledOpenClass>();
            List<IDependency> externalDependencies = getExternalDependencies(source);
            Collection<IDependency> dependencies = CollectionUtils.union(externalDependencies, Arrays.asList(parsedCode.getDependencies()));

            if (dependencies != null && dependencies.size() > 0) {
                if (dependencyManager != null) {
                    for (IDependency dependency : dependencies) {
                        try {
                            CompiledDependency loadedDependency = dependencyManager.loadDependency(dependency);
                            OpenLBundleClassLoader currentClassLoader = (OpenLBundleClassLoader) Thread.currentThread()
                                .getContextClassLoader();
                            currentClassLoader.addClassLoader(loadedDependency.getClassLoader());
                            compiledDependencies.add(loadedDependency.getCompiledOpenClass());
                        } catch (Exception e) {
                            OpenLMessagesUtils.addError(e);                            
                        }
                    }
                } else {
                    OpenLMessagesUtils.addError("Cannot load dependency. Dependency manager is not defined.");                    
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

    private List<IDependency> getExternalDependencies(IOpenSourceCodeModule source) {

        List<IDependency> dependencies = new ArrayList<IDependency>();
        Map<String, Object> params = source.getParams();

        if (params != null) {
            List<IDependency> externalDependencies = (List<IDependency>) params.get(EXTERNAL_DEPENDENCIES_KEY);

            if (externalDependencies != null) {
                dependencies.addAll(externalDependencies);
            }
        }

        return dependencies;
    }
}
