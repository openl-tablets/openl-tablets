package org.openl.rules.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a difference.
 * 
 * @author Andrey Naumenko
 */
public class DiffElement {
    private boolean isSheet;
    private String path;
    private String name;
    private String tooltip;
    private DiffType diffType;
    private List<DiffElement> children = new ArrayList<DiffElement>();
    private boolean isFolder;
    private boolean isXlsFile;

    public boolean getIsSheet() {
        return isSheet;
    }

    public void setIsSheet(boolean isSheet) {
        this.isSheet = isSheet;
    }

    public boolean getIsXlsFile() {
        return isXlsFile;
    }

    public void setIsXlsFile(boolean isXlsFile) {
        this.isXlsFile = isXlsFile;
    }

    public boolean getIsFolder() {
        return isFolder;
    }

    public void setIsFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public DiffType getDiffType() {
        return diffType;
    }

    public void setDiffType(DiffType diffType) {
        this.diffType = diffType;
    }

    public List<DiffElement> getChildren() {
        return children;
    }

    public void setChildren(List<DiffElement> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
}
