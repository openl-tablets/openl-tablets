/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.code.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 * 
 */
public class ParsedCode implements IParsedCode {

    private ISyntaxNode topNode;
    private SyntaxNodeException[] syntaxErrors;
    private IOpenSourceCodeModule source;
    
    private Map<String, Object> params;
    private IDependency[] dependencies;
    
    private Set<CompiledOpenClass> compiledDependencies = new HashSet<CompiledOpenClass>();
    
    private Set<IOpenSourceCodeModule> dependentSources = new HashSet<IOpenSourceCodeModule>();
   
    public ParsedCode(ISyntaxNode topnode, IOpenSourceCodeModule source, SyntaxNodeException[] syntaxErrors) {
        this(topnode, source, syntaxErrors, new IDependency[0]);
    }
    
    public ParsedCode(ISyntaxNode topnode, IOpenSourceCodeModule source, SyntaxNodeException[] syntaxErrors, IDependency[] dependencies) {
        this.topNode = topnode;
        this.syntaxErrors = syntaxErrors;
        this.source = source;
        this.dependencies = dependencies;
    }

    public SyntaxNodeException[] getErrors() {
        return syntaxErrors;
    }

    public IOpenSourceCodeModule getSource() {
        return source;
    }

    public ISyntaxNode getTopNode() {
        return topNode;
    }

    public Map<String, Object> getExternalParams() {
        return params;
    }

    public void setExternalParams(Map<String, Object> params) {
        this.params = params;
    }
    
    //----module dependencies
    public Set<IOpenSourceCodeModule> getDependentSources() {        
        return new HashSet<IOpenSourceCodeModule>(dependentSources);
    }

    public void setDependentSources(Set<IOpenSourceCodeModule> dependentSources) {
        this.dependentSources = new HashSet<IOpenSourceCodeModule>(dependentSources);
    }

    public Set<CompiledOpenClass> getCompiledDependencies() {        
        return new HashSet<CompiledOpenClass>(compiledDependencies);
    }

    public void setCompiledDependencies(Set<CompiledOpenClass> compiledDependencies) {
        this.compiledDependencies = new HashSet<CompiledOpenClass>(compiledDependencies);
    }

    public IDependency[] getDependencies() {
        return dependencies;
    }
}
