package org.openl.syntax.exception;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.StringUtils;

public class SyntaxNodeExceptionCollector {
    private List<SyntaxNodeException> syntaxNodeExceptions = new ArrayList<>();

    public void run(Runnable r, boolean filter) throws Exception {
        try {
            r.run();
        } catch (SyntaxNodeException e) {
            addSyntaxNodeException(e, filter);
        } catch (CompositeSyntaxNodeException e) {
            if (e.getErrors() != null) {
                for (SyntaxNodeException sne : e.getErrors()) {
                    addSyntaxNodeException(sne, filter);
                }
            }
        }
    }

    public void run(Runnable r) throws Exception {
        run(r, false);
    }

    public void addSyntaxNodeException(SyntaxNodeException e) {
        addSyntaxNodeException(e, false);
    }

    public void addSyntaxNodeException(SyntaxNodeException e, boolean filter) {
        if (!filter || filter && !contains(e)) {
            syntaxNodeExceptions.add(e);
        }
    }

    public boolean contains(SyntaxNodeException syntaxNodeException) {
        boolean f = false;
        for (SyntaxNodeException e : syntaxNodeExceptions) {
            if (StringUtils.equals(e.getMessage(),
                syntaxNodeException.getMessage()) && e.getSourceModule() != null && syntaxNodeException
                    .getSourceModule() != null && StringUtils.equals(e.getSourceModule().getUri(),
                        syntaxNodeException.getSourceModule().getUri())) {
                f = true;
                break;
            }
        }
        return f;
    }

    public void throwIfAny() throws SyntaxNodeException {
        throwIfAny(StringUtils.EMPTY);
    }

    public void throwIfAny(String msg) throws SyntaxNodeException {
        if (!syntaxNodeExceptions.isEmpty()) {
            if (syntaxNodeExceptions.size() == 1) {
                throw syntaxNodeExceptions.get(0);
            } else {
                throw new CompositeSyntaxNodeException(msg, syntaxNodeExceptions.toArray(new SyntaxNodeException[] {}));
            }
        }
    }
}
