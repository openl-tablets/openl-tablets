/*
 * Created on May 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.SyntaxErrorException;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class SyntaxError extends Exception implements ISyntaxError {
    /**
     *
     */
    private static final long serialVersionUID = 4448924727461016950L;
    ILocation location;
    String msg;
    Throwable throwable;
    ISyntaxNode syntaxNode;
    ISyntaxNode topLevelSyntaxNode;
    IOpenSourceCodeModule module;

    static public void printSyntaxError(ISyntaxError error, PrintWriter pw) {
        Throwable t = error.getThrowable();
        String msg;
        if (t != null) {
            if (t instanceof SyntaxErrorException) {
                SyntaxErrorException se = (SyntaxErrorException) t;
                for (int i = 0; i < se.getSyntaxErrors().length; i++) {
                    printSyntaxError(se.getSyntaxErrors()[i], pw);
                }

                return;
            }

            msg = error.getMessage();
        } else {
            msg = error.getMessage();
        }

        pw.println("Error: " + msg);

        // if (t t instanceof RuntimeException)
        // t.printStackTrace(stream);

        SourceCodeURLTool.printCodeAndError(error.getLocation(), error.getModule(), pw);

        SourceCodeURLTool.printSourceLocation(error, pw);

        if (error.getThrowable() != null) {
            error.getThrowable().printStackTrace(pw);
        }

    }

    public SyntaxError(ILocation location, String msg, Throwable t, IOpenSourceCodeModule module) {

        this.location = location;
        this.msg = msg;
        throwable = t;
        this.module = module;
    }

    public SyntaxError(ISyntaxNode node, String msg, Throwable t) {
        syntaxNode = node;
        location = node == null ? null : node.getSourceLocation();
        this.msg = msg;
        throwable = t;
    }

    /**
     * @return
     */
    public ILocation getLocation() {
        return location == null ? (syntaxNode == null ? null : syntaxNode.getSourceLocation()) : location;
    }

    /**
     * @return
     */
    @Override
    public String getMessage() {
        Throwable t = getOriginalCause();
        String throwableMessage = t == null ? null : t.getMessage() + " : " + t.getClass().getName();
        return msg == null ? throwableMessage : (throwableMessage == null ? msg : msg + "\n" + throwableMessage);
    }

    /**
     * @return
     */
    public IOpenSourceCodeModule getModule() {
        return module == null ? (syntaxNode == null ? null : syntaxNode.getModule()) : module;
    }

    Throwable getOriginalCause() {
        if (throwable == null) {
            return null;
        }

        Throwable t = ExceptionUtils.getRootCause(throwable);
        return t == null ? throwable : t;

    }

    /**
     * @return
     */
    public ISyntaxNode getSyntaxNode() {
        return syntaxNode;
    }

    /**
     * @return
     */
    public Throwable getThrowable() {
        return throwable;
    }

    public ISyntaxNode getTopLevelSyntaxNode() {
        return topLevelSyntaxNode;
    }

    public String getUri() {
        return SourceCodeURLTool.makeSourceLocationURL(location, module, "");
    }

    public void setTopLevelSyntaxNode(ISyntaxNode topLevelSyntaxNode) {
        this.topLevelSyntaxNode = topLevelSyntaxNode;
    }

    // static public void printCodeAndError(
    // ILocation location,
    // IOpenSourceCodeModule module,
    // PrintWriter pw)
    // {
    // int position = 0;
    //
    // String lineInfo = null;
    //
    // if (location == null)
    // return;
    //
    // if (!location.isTextLocation())
    // {
    // // stream.println(" at " + location);
    // return;
    // }
    //
    // String src = module.getCode();
    // TextInfo info = new TextInfo(src);
    // String[] lines = StringTool.splitLines(src);
    //
    // // position = location.getStart().getAbsolutePosition(info);
    //
    // pw.println("Openl Code Fragment:");
    // pw.println("=======================");
    //
    // int line1 = location.getStart().getLine(info);
    // int column1 = location.getStart().getColumn(info, 1);
    //
    // int line2 = location.getEnd().getLine(info);
    // int column2 = location.getEnd().getColumn(info, 1);
    //
    // int start = Math.max(line1 - 2, 0);
    //
    // int end = Math.min(start + 4, lines.length);
    //
    // for (int i = start; i < end; ++i)
    // {
    // String line = StringTool.untab(lines[i], module.getTabSize());
    // pw.println(line);
    // if (i == line1)
    // {
    // StringBuffer buf =
    // new StringBuffer(Math.max(column1, column2) + 5);
    // StringTool.append(buf, ' ', column1);
    // int col2 = line1 == line2 ? column2 + 1 : line.length();
    //
    // StringTool.append(buf, '^', col2 - column1);
    // pw.println(buf.toString());
    // }
    // }
    // pw.println("=======================");
    //
    // }
    //
    //
    // static public void printSourceLocation(
    // ISyntaxNode node,
    // PrintWriter pw)
    // {
    // printSourceLocation(
    // node.getSourceLocation(),
    // node.getModule(),
    // pw);
    // }
    //
    // static public void printSourceLocation(
    // ISyntaxError error,
    // PrintWriter pw)
    // {
    // printSourceLocation(error.getLocation(), error.getModule(), pw);
    // }
    //
    // static public void printSourceLocation(
    // ILocation location,
    // IOpenSourceCodeModule module,
    // PrintWriter pw)
    // {
    //
    // //TODO fix openl name
    // String url = SourceCodeURLTool.makeSourceLocationURL(location, module,
    // "");
    //
    // //for debug purposes
    // // SourceCodeURLTool.parseUrl(url);
    //
    //
    //
    // pw.println(SourceCodeURLConstants.AT_PREFIX + url);
    //
    // }
    //
    //
    //

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        printSyntaxError(this, pw);

        pw.close();
        // sw.close();
        return sw.toString();
    }

}
