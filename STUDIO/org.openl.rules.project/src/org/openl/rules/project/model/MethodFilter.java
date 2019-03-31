package org.openl.rules.project.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MethodFilter{
    private Collection<String> includes;
    private Collection<String> excludes;

    public Collection<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(Set<String> excludes) {
        this.excludes = excludes;
    }

    public Collection<String> getIncludes() {
        return includes;
    }

    public void setIncludes(Set<String> includes) {
        this.includes = includes;
    }

    public void addIncludePattern(String... patterns) {
        if (patterns != null && patterns.length > 0) {
            if (includes == null) {
                includes = new HashSet<>();
            }
            Collections.addAll(includes, patterns);
        }
    }

    public void addExcludePattern(String... patterns) {
        if (patterns != null && patterns.length > 0) {
            if (excludes == null) {
                excludes = new HashSet<>();
            }
            Collections.addAll(excludes, patterns);
        }
    }

    public void removeAllExcludePatterns() {
        if (excludes != null) {
            this.excludes.clear();
        }
    }

    public void removeAllIncludePatterns() {
        if (includes != null) {
            this.includes.clear();
        }
    }

    public void removeIncludePattern(String pattern) {
        if (includes != null) {
            this.includes.remove(pattern);
        }
    }

    public void removeExcludePattern(String pattern) {
        if (excludes != null) {
            this.excludes.remove(pattern);
        }
    }

}
