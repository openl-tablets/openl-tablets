package org.openl.rules.security.standalone.persistence;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;


/**
 * Base class for domain objects. Provides default implementation of PersistentObject
 * interface and consistent equals/hashCode methods based on object's primary key.
 *
 * @author Andrey Naumenko
 */
public abstract class PersistentObject implements Serializable {
    protected Long id;

    public PersistentObject() {}

    public PersistentObject(Long id) {
        this.id = id;
    }

    /**
     * Primary key.
     *
     * @return key
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object other) {
        if ((other == null) || !this.getClass().isAssignableFrom(other.getClass())) {
            return false;
        }

        PersistentObject castedOther = (PersistentObject) other;

        if (getId() == null) {
            return super.equals(castedOther);
        }

        return getId().equals(castedOther.getId());
    }

    /**
     * {@inheritDoc}
     *
     * @todo it is not good idea to use getKey().hashCode() since key can be changed
     *       (e.g. session.save(object)) after adding object to HashSet
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if (getId() == null) {
            return super.hashCode();
        }
        return getId().hashCode();
    }

    /**
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return new ToStringBuilder(this).append(id).toString();
    }
}
