package org.openl.message;

import java.util.Objects;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.main.SourceCodeURLTool;
import org.openl.source.SourceRange;
import org.openl.syntax.ISyntaxNode;

public class OpenLWarnMessage extends OpenLMessage {

    @Getter
    private final ISyntaxNode source;

    public OpenLWarnMessage(String summary, ISyntaxNode source) {
        super(summary, Severity.WARN);
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public String getSourceLocation() {
        return SourceCodeURLTool.makeSourceLocationURL(source.getSourceLocation(), source.getModule());
    }

    @Override
    public @Nullable SourceRange getSourceRange() {
        var module = source.getModule();
        return module == null ? null : SourceCodeURLTool.rangeOf(source.getSourceLocation(), module);
    }

}
