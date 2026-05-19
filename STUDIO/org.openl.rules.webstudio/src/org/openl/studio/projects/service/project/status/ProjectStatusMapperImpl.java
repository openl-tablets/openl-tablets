package org.openl.studio.projects.service.project.status;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.CompileState;
import org.openl.studio.projects.model.project.status.ModifiedBy;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.project.changes.PendingChangesResolver;
import org.openl.studio.projects.service.project.compile.CompilationJob;
import org.openl.studio.projects.service.project.compile.CompilationStatus;

@Service
@RequiredArgsConstructor
public class ProjectStatusMapperImpl implements ProjectStatusMapper {

    private static final Comparator<MessageDescription> MESSAGE_ORDER = Comparator
            .comparing(MessageDescription::severity)
            .thenComparing(MessageDescription::id);
    private static final List<Severity> SEVERITY_ORDER = List.of(Severity.ERROR, Severity.WARN, Severity.INFO);

    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final WorkspaceProjectService workspaceProjectService;
    private final PendingChangesResolver pendingChangesResolver;

    @Override
    public ProjectStatusViewModel map(RulesProject project) {
        var builder = ProjectStatusViewModel.builder()
                .project(projectIdentifierMapper.map(project).encode());
        if (project.isSupportsBranches()) {
            builder.branch(project.getBranch());
        }
        Optional.ofNullable(project.getFileData()).ifPresent(fileData -> {
            Optional.ofNullable(fileData.getVersion()).ifPresent(builder::revision);
            builder.author(mapAuthor(fileData));
        });
        if (project.isOpened()) {
            var handle = workspaceProjectService.openProject(project);
            var projectModel = handle.project();
            var moduleMessages = projectModel.getModuleMessages();
            builder.compileState(deriveCompileState(handle.compilation(), projectModel, moduleMessages));
            builder.messages(groupMessages(moduleMessages));
        } else {
            builder.compileState(CompileState.IDLE);
        }
        builder.pendingChanges(pendingChangesResolver.resolve(project));
        return builder.build();
    }

    private ModifiedBy mapAuthor(FileData fileData) {
        var authorBuilder = ModifiedBy.builder();
        Optional.ofNullable(fileData.getAuthor())
                .map(UserInfo::getName)
                .ifPresent(authorBuilder::author);
        Optional.ofNullable(fileData.getModifiedAt())
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .ifPresent(authorBuilder::date);
        return authorBuilder.build();
    }

    private CompileState deriveCompileState(CompilationJob compilation,
                                            ProjectModel projectModel,
                                            Collection<OpenLMessage> messages) {
        var status = compilation.status();
        if (status == CompilationStatus.PENDING || status == CompilationStatus.RUNNING) {
            return CompileState.COMPILING;
        }
        if (!projectModel.isProjectCompilationCompleted()) {
            return CompileState.COMPILING;
        }
        var hasWarnings = false;
        for (var message : messages) {
            var severity = message.getSeverity();
            if (severity == Severity.ERROR) {
                return CompileState.ERRORS;
            }
            if (severity == Severity.WARN) {
                hasWarnings = true;
            }
        }
        return hasWarnings ? CompileState.WARNINGS : CompileState.OK;
    }

    private LinkedHashMap<Severity, List<MessageDescription>> groupMessages(Collection<OpenLMessage> messages) {
        var result = new LinkedHashMap<Severity, List<MessageDescription>>();
        for (var severity : SEVERITY_ORDER) {
            var bySeverity = messages.stream()
                    .filter(message -> message.getSeverity() == severity)
                    .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                    .sorted(MESSAGE_ORDER)
                    .toList();
            if (!bySeverity.isEmpty()) {
                result.put(severity, bySeverity);
            }
        }
        return result;
    }
}
