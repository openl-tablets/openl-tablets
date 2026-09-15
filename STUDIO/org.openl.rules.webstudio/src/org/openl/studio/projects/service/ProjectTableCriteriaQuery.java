package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openl.studio.projects.model.tables.TableSearchScope;

/**
 * Project criteria query. Used to filter project tables in {@link ProjectService}.
 *
 * @author Vladyslav Pikus
 */
public class ProjectTableCriteriaQuery {

    private final Collection<String> kinds;
    private final String name;
    private final String module;
    private final String header;
    private final String text;
    private final TableSearchScope scope;
    private final Map<String, Object> properties;
    private final boolean includeOther;

    private ProjectTableCriteriaQuery(Builder builder) {
        this.kinds = builder.kinds == null ? List.of()
                : Collections.unmodifiableCollection(builder.kinds);
        this.name = builder.name;
        this.module = builder.module;
        this.header = builder.header;
        this.text = builder.text;
        this.scope = builder.scope;
        this.properties = builder.properties == null ? Map.of() : Map.copyOf(builder.properties);
        this.includeOther = builder.includeOther;
    }

    public Collection<String> getKinds() {
        return kinds;
    }

    /**
     * Tells whether free-form tables take part in the search.
     *
     * <p>They are left out by default: a table OpenL does not recognize carries no kind, no name and no properties to
     * match on, so it only adds noise to a browsing query.
     */
    public boolean isIncludeOther() {
        return includeOther;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Module whose tables are asked for, when the search is narrowed to one.
     *
     * <p>Empty means the whole project takes part in the search.
     */
    public Optional<String> getModule() {
        return Optional.ofNullable(module);
    }

    /**
     * Text the table's header line must contain, as the table was written.
     *
     * <p>The header is the line a table starts with — its keyword, what it returns and the arguments it takes —
     * so this is what a reader searches by to find a table by its signature rather than by its name.
     */
    public Optional<String> getHeader() {
        return Optional.ofNullable(header);
    }

    /**
     * Text any cell of the table must contain.
     *
     * <p>Every cell of every table in scope is read for it, so it is the slowest thing to ask for and the only
     * one that finds a table by what is written inside it.
     */
    public Optional<String> getText() {
        return Optional.ofNullable(text);
    }

    /**
     * How wide the search reaches.
     *
     * <p>Empty means as wide as the request implies: the module it names, or the whole project when it names
     * none.
     */
    public Optional<TableSearchScope> getScope() {
        return Optional.ofNullable(scope);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Collection<String> kinds;
        private String name;
        private String module;
        private String header;
        private String text;
        private TableSearchScope scope;
        private Map<String, Object> properties = new HashMap<>();
        private boolean includeOther;

        public Builder kinds(Collection<String> kinds) {
            this.kinds = kinds;
            return this;
        }

        public Builder includeOther(boolean includeOther) {
            this.includeOther = includeOther;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder header(String header) {
            this.header = header;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder scope(TableSearchScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder property(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }

        public ProjectTableCriteriaQuery build() {
            return new ProjectTableCriteriaQuery(this);
        }
    }

}
