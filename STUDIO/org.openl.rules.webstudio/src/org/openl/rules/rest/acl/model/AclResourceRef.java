package org.openl.rules.rest.acl.model;

import static org.openl.util.StringUtils.isBlank;
import static org.openl.util.StringUtils.isNotBlank;

import java.beans.Transient;
import java.util.Comparator;
import java.util.Objects;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.security.acl.repository.AclRepositoryType;

@Schema(description = "Identifies the target resource to apply ACL rules")
@JsonDeserialize(builder = AclResourceRef.Builder.class)
public class AclResourceRef {

    public static final Comparator<AclResourceRef> COMPARATOR = Comparator.comparing(AclResourceRef::getRepositoryType)
            .thenComparing(AclResourceRef::getRepositoryId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AclResourceRef::getProjectName, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Parameter(description = "Type of the repository (e.g., DESIGN, PROD)")
    @NotNull
    private final AclRepositoryType repositoryType;

    @Parameter(description = "Repository ID")
    private final String repositoryId;

    @Parameter(description = "Optional project name inside the repository")
    private final String projectName;

    private AclResourceRef(Builder builder) {
        this.repositoryId = builder.repositoryId;
        this.projectName = builder.projectName;
        this.repositoryType = builder.repositoryType;
    }

    public AclRepositoryType getRepositoryType() {
        return repositoryType;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getProjectName() {
        return projectName;
    }

    @Transient
    public boolean isRepositoryRoot() {
        return repositoryType != null && isBlank(repositoryId) && isBlank(projectName);
    }

    @Transient
    public boolean isRepository() {
        return repositoryType != null && isNotBlank(repositoryId) && isBlank(projectName);
    }

    @Transient
    public boolean isProject() {
        // ignore repositoryType for project check because it's always DESIGN
        return isNotBlank(repositoryId) && isNotBlank(projectName);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AclResourceRef that = (AclResourceRef) o;
        return repositoryType == that.repositoryType
                && Objects.equals(repositoryId, that.repositoryId)
                && Objects.equals(projectName, that.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryType, repositoryId, projectName);
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String repositoryId;
        private String projectName;
        private AclRepositoryType repositoryType = AclRepositoryType.DESIGN;

        private Builder() {
        }

        public Builder repositoryId(String repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder repositoryType(AclRepositoryType repositoryType) {
            this.repositoryType = repositoryType;
            return this;
        }

        public AclResourceRef build() {
            return new AclResourceRef(this);
        }
    }

}
