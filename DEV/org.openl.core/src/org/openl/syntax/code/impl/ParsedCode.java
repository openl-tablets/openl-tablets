/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.code.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
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
    private Collection<OpenLMessage> messages;
    private IOpenSourceCodeModule source;

    private Map<String, Object> params;
    private IDependency[] dependencies;

    private Set<CompiledDependency> compiledDependencies = new HashSet<CompiledDependency>();

    public ParsedCode(ISyntaxNode topnode,
            IOpenSourceCodeModule source,
            SyntaxNodeException[] syntaxErrors,
            Collection<OpenLMessage> messages) {
        this(topnode, source, syntaxErrors, messages, new IDependency[0]);
    }

    public ParsedCode(ISyntaxNode topNode,
            IOpenSourceCodeModule source,
            SyntaxNodeException[] syntaxErrors,
            Collection<OpenLMessage> messages,
            IDependency[] dependencies) {
        this.topNode = topNode;
        this.syntaxErrors = syntaxErrors;
        this.source = source;
        if (messages == null) {
            this.messages = Collections.emptyList();
        } else {
            this.messages = new LinkedHashSet<>(messages);
        }
        this.dependencies = dependencies;
    }

    public SyntaxNodeException[] getErrors() {
        return syntaxErrors;
    }

    public Collection<OpenLMessage> getMessages() {
        return Collections.unmodifiableCollection(messages);
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

    public IDependency[] getDependencies() {
        return dependencies;
    }
}
