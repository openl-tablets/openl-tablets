package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "OpenL_Projects")
public class OpenLProject implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String repositoryId;
    private String projectPath;
    private List<Tag> tags;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "OpenL_Projects_ID_SEQ")
    @SequenceGenerator(sequenceName = "OpenL_Projects_ID_SEQ", name = "OpenL_Projects_ID_SEQ")
    @Column(name = "id")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "repository_id")
    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Column(name = "project_path")
    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    @ManyToMany(targetEntity = Tag.class, fetch = FetchType.EAGER, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "OpenL_Project_Tags", joinColumns = { @JoinColumn(name = "project_id") }, inverseJoinColumns = {
            @JoinColumn(name = "tag_id") })
    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OpenLProject))
            return false;
        OpenLProject that = (OpenLProject) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
