package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Project criteria query. Used to filter project tables in {@link ProjectService}.
 *
 * @author Vladyslav Pikus
 */
public class ProjectTableCriteriaQuery {

    private final Collection<String> kinds;
    private final String name;
    private final Map<String, Object> properties;
    private final boolean includeOther;

    private ProjectTableCriteriaQuery(Builder builder) {
        this.kinds = builder.kinds == null ? Collections.emptyList()
                : Collections.unmodifiableCollection(builder.kinds);
        this.name = builder.name;
        this.properties = builder.properties == null ? Collections.emptyMap() : Map.copyOf(builder.properties);
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

    public Map<String, Object> getProperties() {
        return properties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Collection<String> kinds;
        private String name;
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

        public Builder property(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }

        public ProjectTableCriteriaQuery build() {
            return new ProjectTableCriteriaQuery(this);
        }
    }

}
