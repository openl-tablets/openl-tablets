package org.openl.generated.beans;

import java.util.Objects;

public class Coverage {

    protected String name;
    protected Double limit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLimit() {
        return limit;
    }

    public void setLimit(Double limit) {
        this.limit = limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coverage coverage = (Coverage) o;
        return Objects.equals(name, coverage.name) &&
                Objects.equals(limit, coverage.limit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, limit);
    }

    @Override
    public String toString() {
        return "Coverage{" +
                "name='" + name + '\'' +
                ", limit=" + limit +
                '}';
    }
}
