package org.openl.message;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.main.SourceCodeURLTool;
import org.openl.source.SourceRange;
import org.openl.syntax.ISyntaxNode;

/**
 * A message of a linter: a check of what the tables of a project mean, made apart from their compilation.
 *
 * <p>The rules compile and run as they are written. Such a message only points at something to tidy up, such as a
 * field no table uses, so it is an information message.
 *
 * <p>It tells the place it is about as the place stood when the check ran, and keeps nothing of the compilation. A
 * message may be kept until the next check, while the project compiles again.
 */
@Getter
public final class OpenLLintMessage extends OpenLMessage {

    private final String sourceLocation;
    private final @Nullable SourceRange sourceRange;

    public OpenLLintMessage(String summary, ISyntaxNode source) {
        super(summary, Severity.INFO);
        var module = source.getModule();
        this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(source.getSourceLocation(), module);
        this.sourceRange = module == null ? null : SourceCodeURLTool.rangeOf(source.getSourceLocation(), module);
    }
}
