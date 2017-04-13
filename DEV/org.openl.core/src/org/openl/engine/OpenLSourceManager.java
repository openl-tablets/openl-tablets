package org.openl.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceType;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;

/**
 * Class that defines OpenL engine manager implementation for source processing
 * operations.
 * 
 */
public class OpenLSourceManager extends OpenLHolder {

    public static final String EXTERNAL_DEPENDENCIES_KEY = "external-dependencies";

    public static final String ADDITIONAL_WARN_MESSAGES_KEY = "additional-warn-messages";
    public static final String ADDITIONAL_ERROR_MESSAGES_KEY = "additional-error-messages";

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
    public ProcessedCode processSource(IOpenSourceCodeModule source,
            SourceType sourceType,
            IDependencyManager dependencyManager) {
        return processSource(source, sourceType, null, false, dependencyManager);
    }

    private Collection<IDependency> getDependencies(IDependencyManager dependencyManager, IDependency[] dependencies){
        Set<IDependency> result = new LinkedHashSet<IDependency>();
        if (dependencyManager == null){
            result.addAll(Arrays.asList(dependencies));
            return result;
        }
        Collection<String> dependencyNames;
        try{
             dependencyNames = dependencyManager.getAllDependencies();
            if (dependencyNames == null){
                result.addAll(Arrays.asList(dependencies));    
                return result;
            }
        }catch(Exception e){
            return Arrays.asList(dependencies);
        }
        for (IDependency dependency : dependencies){
            String value = dependency.getNode().getIdentifier();
            value = value.replaceAll("\\*", "\\\\E.*\\\\Q");
            value = value.replaceAll("\\?", "\\\\E.\\\\Q");
            value = "\\Q" + value + "\\E";

            boolean found = false;
            for (String dependencyName : dependencyNames){
                if (Pattern.matches(value, dependencyName)){
                    found = true;
                    result.add(new Dependency(dependency.getType(),
                        new IdentifierNode(dependency.getNode().getType(), null, dependencyName, null)));
                }
            }

            if (!found) {
                // Needed to create error message "Dependency wasn't found" later
                result.add(dependency);
            }

        }
        return result;
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

            Set<CompiledDependency> compiledDependencies = new LinkedHashSet<CompiledDependency>();

            Collection<IDependency> externalDependencies = getExternalDependencies(source);

            Collection<IDependency> dependencies = getDependencies(dependencyManager, parsedCode.getDependencies());
            if (CollectionUtils.isNotEmpty(externalDependencies)) {
                dependencies.addAll(externalDependencies);
            }

            List<OpenLMessage> messages = new ArrayList<OpenLMessage>(OpenLMessages.getCurrentInstance().getMessages());
            List<OpenLMessage> dependencyMessages = new ArrayList<OpenLMessage>();
            
            if (CollectionUtils.isNotEmpty(dependencies)) {
                if (dependencyManager != null) {
                    for (IDependency dependency : dependencies) {
                        try {
                            CompiledDependency loadedDependency = dependencyManager.loadDependency(dependency);
                            validateDependency(loadedDependency);
                            OpenLBundleClassLoader currentClassLoader = (OpenLBundleClassLoader) Thread.currentThread()
                                .getContextClassLoader();
                            if (loadedDependency.getClassLoader() != currentClassLoader) {
                                currentClassLoader.addClassLoader(loadedDependency.getClassLoader());
                            }
                            compiledDependencies.add(loadedDependency);

                            CompiledOpenClass compiledOpenClass = loadedDependency.getCompiledOpenClass();
                            IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
                            if (openClass instanceof ExtendableModuleOpenClass){
                                ExtendableModuleOpenClass extendableModuleOpenClass = (ExtendableModuleOpenClass) openClass;
                                extendableModuleOpenClass.applyToDependentParsedCode(parsedCode);
                            }
                            
                            for (OpenLMessage message : compiledOpenClass.getMessages()) { // Save messages from dependencies
                                if (!dependencyMessages.contains(message)) {
                                    dependencyMessages.add(message);
                                }
                            }

                            OpenLMessages.getCurrentInstance().clear();// clear
                                                                       // all
                                                                       // messages
                                                                       // from
                                                                       // dependency

                        } catch (Exception e) {
                            messages.addAll(OpenLMessagesUtils.newMessages(e));
                        }
                    }
                    OpenLMessages.getCurrentInstance().addMessages(messages);
                } else {
                    OpenLMessagesUtils.addError("Can't load dependencies. Dependency manager is not defined.");
                }
            }

            parsedCode.setCompiledDependencies(compiledDependencies);
            parsedCode.setMessagesFromDependencies(dependencyMessages);
        }

        Map<String, Object> externalParams = source.getParams();

        if (externalParams != null) {
            parsedCode.setExternalParams(externalParams);
            if (externalParams.containsKey(ADDITIONAL_WARN_MESSAGES_KEY)) {
                @SuppressWarnings("unchecked")
                Set<String> warnMessages = (Set<String>) externalParams.get(ADDITIONAL_WARN_MESSAGES_KEY);
                for (String warnMessage : warnMessages) {
                    OpenLMessagesUtils.addWarn(warnMessage);
                }
            }
            if (externalParams.containsKey(ADDITIONAL_ERROR_MESSAGES_KEY)) {
                @SuppressWarnings("unchecked")
                Set<String> warnMessages = (Set<String>) externalParams.get(ADDITIONAL_ERROR_MESSAGES_KEY);
                for (String warnMessage : warnMessages) {
                    OpenLMessagesUtils.addError(warnMessage);
                }
            }
        }
        
        //Requires to support java packages. BEX grammar doesn't support to use binding context to define java packages.
        FullClassnameSupport.transformIdentifierBindersWithBindingContextInfo(bindingContextDelegator, parsedCode);
        
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

    private void validateDependency(CompiledDependency compiledDependency) {
        if (compiledDependency.getCompiledOpenClass().hasErrors()) {
            String message = String.format("Dependency module %s has critical errors",
                compiledDependency.getDependencyName());
            OpenLMessagesUtils.addError(message);
        }
    }

    private List<IDependency> getExternalDependencies(IOpenSourceCodeModule source) {
        List<IDependency> dependencies = null;
        Map<String, Object> params = source.getParams();

        if (params != null) {
            dependencies = (List<IDependency>) params.get(EXTERNAL_DEPENDENCIES_KEY);

        }
        return dependencies;
    }
}
