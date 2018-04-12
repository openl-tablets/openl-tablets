/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.code.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.dependency.CompiledDependency;
import org.openl.message.IOpenLMessages;
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
    
    IOpenLMessages messagesFromDependencies;
    
    private Set<CompiledDependency> compiledDependencies = new HashSet<CompiledDependency>();
    
    public ParsedCode(ISyntaxNode topnode, IOpenSourceCodeModule source, SyntaxNodeException[] syntaxErrors) {
        this(topnode, source, syntaxErrors, new IDependency[0]);
    }
    
    public ParsedCode(ISyntaxNode topNode, IOpenSourceCodeModule source, SyntaxNodeException[] syntaxErrors, IDependency[] dependencies) {
        this.topNode = topNode;
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
    
    public Set<CompiledDependency> getCompiledDependencies() {        
        return new HashSet<CompiledDependency>(compiledDependencies);
    }

    public void setCompiledDependencies(Set<CompiledDependency> compiledDependencies) {
        this.compiledDependencies = new HashSet<CompiledDependency>(compiledDependencies);
    }
    
    public void setMessagesFromDependencies(IOpenLMessages messagesFromDependencies){
        this.messagesFromDependencies = messagesFromDependencies;
    }
    
    public IOpenLMessages getMessagesFromDependencies(){
        return messagesFromDependencies;
    }

    public IDependency[] getDependencies() {
        return dependencies;
    }
}
