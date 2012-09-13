package org.openl.rules.project.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MethodFilter {
    private Collection<String> includes = new HashSet<String>();
    private Collection<String> excludes = new HashSet<String>();

    public Collection<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(Set<String> excludes) {
        if (excludes == null) {
            this.excludes = new HashSet<String>();
        } else {
            this.excludes = excludes;
        }
    }

    public Collection<String> getIncludes() {
        return includes;
    }

    public void setIncludes(Set<String> includes) {
        if (includes == null) {
            this.includes = new HashSet<String>();
        } else {
            this.includes = includes;
        }
    }

    public void addIncludePattern(String pattern) {
        if (pattern != null) {
            includes.add(pattern);
        }
    }

    public void addExcludePattern(String pattern) {
        if (pattern != null) {
            excludes.add(pattern);
        }
    }

    public void removeAllExcludePatterns() {
        this.excludes.clear();
    }

    public void removeAllIncludePatterns() {
        this.includes.clear();
    }

    public void removeIncludePattern(String pattern) {
        this.includes.remove(pattern);
    }

    public void removeExcludePattern(String pattern) {
        this.excludes.remove(pattern);
    }
}
