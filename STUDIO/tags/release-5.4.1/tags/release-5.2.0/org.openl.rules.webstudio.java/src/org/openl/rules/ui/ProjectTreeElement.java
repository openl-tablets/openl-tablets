package org.openl.rules.ui;

import java.util.Iterator;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.TreeElement;

public class ProjectTreeElement extends TreeElement implements INamedThing {
    String uri;
    String[] displayName;
    int nameCount = 0;

    TableSyntaxNode tsn;

    Object problem;

    public ProjectTreeElement(String[] displayName, String type, String uri, Object problems, int nameCount,
            TableSyntaxNode tsn) {
        super(type);
        this.uri = uri;
        this.displayName = displayName;
        problem = problems;
        this.nameCount = nameCount;
        this.tsn = tsn;
    }

    public String[] getDisplayName() {
        return displayName;
    }

    public String getDisplayName(int mode) {
        return displayName[mode];
    }

    public String getName() {
        return getDisplayName(SHORT);
    }

    public int getNameCount() {
        return nameCount;
    }

    public Object getProblem() {
        return problem;
    }

    public String getUri() {
        return uri;
    }

    public boolean hasProblem() {
        if (problem != null) {
            return true;
        }

        for (Iterator iter = getChildren(); iter.hasNext();) {
            ProjectTreeElement pt = (ProjectTreeElement) iter.next();
            if (pt.hasProblem()) {
                return true;
            }
        }

        return false;

    }

    public void setProblem(Object problem) {
        this.problem = problem;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

}
