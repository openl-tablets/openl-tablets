package org.openl.rules.webstudio.web.repository.project;

import java.io.InputStream;

public class ProjectFile {

    private String name;
    private InputStream input;
    private long size;

    public ProjectFile() {
    }

    public ProjectFile(String name, InputStream input) {
        this.name = name;
        this.input = input;
    }

    public ProjectFile(String name, InputStream input, long size) {
        this.name = name;
        this.input = input;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
