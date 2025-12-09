package org.openl.studio.projects.model;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.converter.ProjectStatusSerializer;
import org.openl.util.StringUtils;

public abstract class AProjectViewModel {

    @Parameter(description = "Project Name", required = true)
    @JsonView(GenericView.Full.class)
    public final String name;

    @Parameter(description = "Author of latest update", required = true)
    @JsonView(GenericView.Full.class)
    public final String modifiedBy;

    @Parameter(description = "Date and time of latest update", required = true)
    @JsonView(GenericView.Full.class)
    public final ZonedDateTime modifiedAt;

    @Parameter(description = "Lock info")
    @JsonView(GenericView.Full.class)
    public final ProjectLockInfo lockInfo;

    @Parameter(description = "Branch Name. Can be absent if current repository doesn't support branches")
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    public final String branch;

    @Parameter(description = "Revision ID", required = true)
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    public final String revision;

    @Parameter(description = "Project path in target repository. Can be absent if Design Repository is flat")
    @JsonView(GenericView.Full.class)
    public final String path;

    @Parameter(description = "Project identifier", required = true)
    @JsonView(GenericView.Full.class)
    public final ProjectIdModel id;

    @Parameter(description = "Project Status", schema = @Schema(allowableValues = {"LOCAL",
            "ARCHIVED",
            "OPENED",
            "VIEWING_VERSION",
            "EDITING",
            "CLOSED"}))
    @JsonSerialize(using = ProjectStatusSerializer.class)
    @JsonView(GenericView.Full.class)
    public final ProjectStatus status;

    @Parameter(description = "Source Repository")
    @JsonView(GenericView.Full.class)
    public final String repository;

    protected AProjectViewModel(ABuilder<?> from) {
        this.name = from.name;
        this.modifiedBy = from.modifiedBy;
        this.modifiedAt = from.modifiedAt;
        this.branch = from.branch;
        this.revision = from.revision;
        this.path = from.path;
        this.id = from.id;
        this.status = from.status;
        this.repository = from.repository;
        this.lockInfo = from.lockInfo;
    }

    protected static abstract class ABuilder<T extends ABuilder<T>> {
        protected String name;
        protected String modifiedBy;
        protected ZonedDateTime modifiedAt;
        protected String branch;
        protected String revision;
        protected String path;
        protected ProjectIdModel id;
        protected ProjectStatus status;
        protected String repository;
        protected ProjectLockInfo lockInfo;

        protected ABuilder() {

        }

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
            return self();
        }

        public T lockInfo(ProjectLockInfo lockInfo) {
            this.lockInfo = lockInfo;
            return self();
        }

        public T modifiedAt(ZonedDateTime modifiedAt) {
            this.modifiedAt = modifiedAt;
            return self();
        }

        public T branch(String branch) {
            this.branch = branch;
            return self();
        }

        public T revision(String revision) {
            this.revision = revision;
            return self();
        }

        public T path(String path) {
            this.path = StringUtils.isEmpty(path) ? null : path;
            return self();
        }

        public T id(ProjectIdModel id) {
            this.id = id;
            return self();
        }

        public T status(ProjectStatus status) {
            this.status = status;
            return self();
        }

        public T repository(String repository) {
            this.repository = repository;
            return self();
        }

        protected abstract T self();
    }
}
