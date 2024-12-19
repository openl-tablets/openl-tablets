package org.openl.rules.rest.acl.model;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;

@Schema(description = "ACL Repository Model")
public class AclRepositoryModel {

    @JsonView({AclView.Repository.class,  AclView.Root.class})
    @Parameter(description = "Repository ID")
    private final AclRepositoryId id;

    @JsonView(AclView.Repository.class)
    @Parameter(description = "Repository Name")
    private final String name;

    @JsonView({AclView.Repository.class,  AclView.Root.class})
    @Parameter(description = "Repository Type")
    private final AclRepositoryType type;

    @JsonView(AclView.Sid.class)
    @Parameter(description = "SID")
    private final AclSidModel sid;

    @NotNull
    @JsonView({AclView.Repository.class, AclView.Root.class, AclView.Sid.class})
    @Parameter(description = "Role")
    private final AclRole role;

    private AclRepositoryModel(RootRepositoryBuilder<?> builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.role = builder.role;
        this.name = null;
        this.sid = null;
    }

    private AclRepositoryModel(RepositoryBuilder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.role = builder.role;
        this.name = builder.name;
        this.sid = null;
    }

    private AclRepositoryModel(SidRepositoryBuilder builder) {
        this.id = null;
        this.type = null;
        this.role = builder.role;
        this.name = null;
        this.sid = builder.sid;
    }

    public AclRepositoryId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AclRepositoryType getType() {
        return type;
    }

    public AclRole getRole() {
        return role;
    }

    public AclSidModel getSid() {
        return sid;
    }

    public static RootRepositoryBuilder<?> rootRepositoryBuilder() {
        return new RootRepositoryBuilder<>();
    }

    public static RepositoryBuilder repositoryBuilder() {
        return new RepositoryBuilder();
    }

    public static SidRepositoryBuilder sidRepositoryBuilder() {
        return new SidRepositoryBuilder();
    }

    private static abstract class ABuilder<T extends ABuilder<T>> {

        protected AclRole role;

        protected ABuilder() {
        }

        public T role(AclRole role) {
            this.role = role;
            return self();
        }

        @Nonnull
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        @Nonnull
        public abstract AclRepositoryModel build();
    }

    public static class RootRepositoryBuilder<T extends RootRepositoryBuilder<T>> extends ABuilder<T> {

        protected AclRepositoryId id;
        protected AclRepositoryType type;

        protected RootRepositoryBuilder() {
        }

        public T id(AclRepositoryId id) {
            this.id = id;
            return self();
        }

        public T type(AclRepositoryType type) {
            this.type = type;
            return self();
        }

        @Nonnull
        @Override
        public AclRepositoryModel build() {
            return new AclRepositoryModel(this);
        }
    }

    public static class RepositoryBuilder extends RootRepositoryBuilder<RepositoryBuilder> {
        private String name;

        private RepositoryBuilder() {
        }

        public RepositoryBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Nonnull
        @Override
        public AclRepositoryModel build() {
            return new AclRepositoryModel(this);
        }
    }

    public static class SidRepositoryBuilder extends ABuilder<SidRepositoryBuilder> {
        private AclSidModel sid;

        private SidRepositoryBuilder() {
        }

        public SidRepositoryBuilder sid(AclSidModel sid) {
            this.sid = sid;
            return this;
        }

        @Nonnull
        @Override
        public AclRepositoryModel build() {
            return new AclRepositoryModel(this);
        }
    }
}
