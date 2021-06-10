package org.openl.rules.ui.tablewizard;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Aliaksandr Antonik.
 */
public class TypeNamePair {

    @NotBlank(message = "Cannot be empty")
    private String type;

    private boolean iterable;

    @NotBlank(message = "Cannot be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = "Invalid name for parameter: only latin letters, numbers and _ are allowed, name cannot begin with a number")
    private String name;

    private String submittedName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isIterable() {
        return iterable;
    }

    public void setIterable(boolean iterable) {
        this.iterable = iterable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubmittedName() {
        return submittedName;
    }

    public void setSubmittedName(String submittedName) {
        this.submittedName = submittedName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof TypeNamePair)) {
            return false;
        }

        TypeNamePair tnp = (TypeNamePair) obj;
        if (this.name == null && tnp.getName() != null) {
            return false;
        }

        if (this.name == null && tnp.getName() == null && this.type == null && tnp.type == null && this
            .isIterable() == tnp.isIterable()) {
            return true;
        }

        if (this.name == null && tnp.getName() == null && this.type.equals(tnp.type) && this.isIterable() == tnp
            .isIterable()) {
            return true;
        }

        if (this.name.equals(tnp.getName()) && this.type == null & tnp.type == null && this.isIterable() == tnp
            .isIterable()) {
            return true;
        }

        if (this.name.equals(tnp.getName()) && this.type.equals(tnp.type) && this.isIterable() == tnp.isIterable()) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        String forHashCode = this.name + this.type + this.isIterable();

        return forHashCode.hashCode();
    }

}
