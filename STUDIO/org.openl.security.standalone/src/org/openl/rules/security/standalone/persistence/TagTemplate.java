package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "OpenL_Tag_Templates")
public class TagTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    private String template;
    private int priority;

    @Id
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TagTemplate))
            return false;
        TagTemplate that = (TagTemplate) o;
        return getTemplate().equals(that.getTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTemplate());
    }
}
