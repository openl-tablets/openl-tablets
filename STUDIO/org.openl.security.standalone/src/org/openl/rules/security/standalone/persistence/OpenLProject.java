package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "OpenL_Projects")
public class OpenLProject implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String repositoryId;
    private String projectPath;
    private List<Tag> tags;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
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

    @ManyToMany(targetEntity = Tag.class, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "OpenL_Project_Tags", joinColumns = {@JoinColumn(name = "project_id")}, inverseJoinColumns = {
            @JoinColumn(name = "tag_id")})
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
