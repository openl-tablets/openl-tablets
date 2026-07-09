package org.openl.studio.projects.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.ProjectDescriptorView;
import org.openl.studio.projects.model.ProjectDescriptorView.DependencyView;
import org.openl.studio.projects.model.ProjectDescriptorView.ExposedMethodView;
import org.openl.studio.projects.model.ProjectDescriptorView.MethodFilterView;
import org.openl.studio.projects.model.ProjectDescriptorView.ModuleView;
import org.openl.studio.projects.model.ProjectDescriptorView.OpenApiView;

/**
 * Reads and writes a project's {@code rules.xml} descriptor as a stable view model, keeping the
 * JAXB codec, the editability check and the concurrency guard on the server. The React Project page
 * is the client of this service.
 *
 * @author Yury Molchan
 */
@Service
@RequiredArgsConstructor
public class ProjectDescriptorService {

    @Qualifier("designRepositoryAclService")
    private final RepositoryAclService designRepositoryAclService;

    /**
     * Returns the editable descriptor of the project. When the project has no {@code rules.xml}, an
     * empty editable model is returned so the client can start from a blank descriptor.
     *
     * @param project the project whose descriptor is read; READ access is already enforced upstream
     * @return the descriptor view, with the editability flag and the content hash for concurrency
     */
    public ProjectDescriptorView getDescriptor(RulesProject project) {
        boolean editable = designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE));
        byte[] content = readDescriptorContent(project);
        if (content.length == 0) {
            return map(new ProjectDescriptor(), editable, "");
        }
        ProjectDescriptor descriptor = ProjectDescriptor.read(new ByteArrayInputStream(content));
        if (descriptor == null) {
            throw new ConflictException("project.descriptor.invalid.message");
        }
        return map(descriptor, editable, sha256(content));
    }

    /**
     * Writes the edited descriptor to {@code rules.xml} as a whole document and returns the fresh
     * view (with the new content hash).
     *
     * <p>Requires design-repository WRITE permission. The write is guarded by an optimistic content
     * hash: unless {@code force} is set, the current {@code rules.xml} must still match the hash the
     * client loaded, otherwise a {@link ConflictException} (409) signals a concurrent change so the
     * client can confirm an overwrite. Recompilation is triggered by the caller after this returns.
     *
     * @param project the project to write; WRITE access is enforced here
     * @param model   the full edited descriptor, carrying the loaded content hash
     * @param force   overwrite even when the content hash no longer matches
     * @return the descriptor view read back after the write
     */
    public ProjectDescriptorView updateDescriptor(RulesProject project, ProjectDescriptorView model, boolean force) {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("project.descriptor.write.forbidden.message");
        }
        byte[] current = readDescriptorContent(project);
        String currentHash = current.length == 0 ? "" : sha256(current);
        if (!force && !Objects.equals(currentHash, model.contentHash() == null ? "" : model.contentHash())) {
            throw new ConflictException("project.descriptor.stale.message");
        }
        writeDescriptor(project, toDescriptor(model).toBytes());
        return getDescriptor(project);
    }

    private void writeDescriptor(RulesProject project, byte[] bytes) {
        try {
            if (project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
                var artefact = (AProjectResource) project.getArtefact(ProjectDescriptor.FILE_NAME);
                artefact.setContent(new ByteArrayInputStream(bytes));
            } else {
                project.addResource(ProjectDescriptor.FILE_NAME, new ByteArrayInputStream(bytes));
                var artefact = project.getArtefact(ProjectDescriptor.FILE_NAME);
                if (!designRepositoryAclService.hasAcl(artefact)) {
                    designRepositoryAclService.createAcl(artefact,
                            List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
                }
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.descriptor.write.failed.message");
        }
    }

    private static byte[] readDescriptorContent(RulesProject project) {
        try {
            if (!project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
                return new byte[0];
            }
            AProjectArtefact artefact = project.getArtefact(ProjectDescriptor.FILE_NAME);
            if (!(artefact instanceof AProjectResource resource)) {
                return new byte[0];
            }
            try (InputStream in = resource.getContent()) {
                return in.readAllBytes();
            }
        } catch (ProjectException | IOException e) {
            throw new ConflictException("project.descriptor.read.failed.message");
        }
    }

    private static ProjectDescriptor toDescriptor(ProjectDescriptorView model) {
        var descriptor = new ProjectDescriptor();
        descriptor.setName(model.name());
        descriptor.setComment(model.comment());
        descriptor.setModules(toModules(model.modules()));
        descriptor.setClasspath(model.classpath() == null ? null : new ArrayList<>(model.classpath()));
        descriptor.setDependencies(toDependencies(model.dependencies()));
        descriptor.setOpenapi(toOpenApi(model.openapi()));
        descriptor.setExposedMethods(toExposedMethods(model.exposedMethods()));
        descriptor.setPropertiesFileNameProcessor(model.propertiesFileNameProcessor());
        descriptor.setPropertiesFileNamePatterns(
                model.propertiesFileNamePatterns() == null
                        ? null
                        : model.propertiesFileNamePatterns().toArray(new String[0]));
        return descriptor;
    }

    private static List<Module> toModules(@Nullable List<ModuleView> modules) {
        var result = new ArrayList<Module>();
        if (modules == null) {
            return result;
        }
        for (ModuleView view : modules) {
            var module = new Module();
            module.setName(view.name());
            module.setRulesRootPath(view.rulesRootPath());
            module.setMethodFilter(toMethodFilter(view.methodFilter()));
            module.getWebstudioConfiguration().setCompileThisModuleOnly(view.compileThisModuleOnly());
            result.add(module);
        }
        return result;
    }

    private static @Nullable MethodFilter toMethodFilter(@Nullable MethodFilterView view) {
        if (view == null) {
            return null;
        }
        var filter = new MethodFilter();
        filter.setIncludes(toSet(view.includes()));
        filter.setExcludes(toSet(view.excludes()));
        return filter;
    }

    private static @Nullable List<ProjectDependencyDescriptor> toDependencies(@Nullable List<DependencyView> deps) {
        if (deps == null) {
            return null;
        }
        var result = new ArrayList<ProjectDependencyDescriptor>();
        for (DependencyView view : deps) {
            var dependency = new ProjectDependencyDescriptor();
            dependency.setName(view.name());
            dependency.setAutoIncluded(view.autoIncluded());
            dependency.setMavenArtifact(view.mavenArtifact());
            result.add(dependency);
        }
        return result;
    }

    private static @Nullable OpenAPI toOpenApi(@Nullable OpenApiView view) {
        if (view == null) {
            return null;
        }
        var mode = view.mode() == null ? null : OpenAPI.Mode.valueOf(view.mode());
        return new OpenAPI(view.path(), mode, view.modelModuleName(), view.algorithmModuleName());
    }

    private static @Nullable ExposedMethods toExposedMethods(@Nullable List<ExposedMethodView> views) {
        if (views == null || views.isEmpty()) {
            return null;
        }
        var byType = views.stream()
                .filter(view -> view.pattern() != null && !view.pattern().isBlank())
                .collect(Collectors.groupingBy(
                        view -> "exclude".equals(view.type()) ? "exclude" : "include",
                        Collectors.mapping(ExposedMethodView::pattern, Collectors.toCollection(HashSet::new))));
        var methods = new ExposedMethods();
        methods.setIncludes(byType.getOrDefault("include", new HashSet<>()));
        methods.setExcludes(byType.getOrDefault("exclude", new HashSet<>()));
        return methods;
    }

    private static Set<String> toSet(@Nullable List<String> values) {
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    private static ProjectDescriptorView map(ProjectDescriptor descriptor, boolean editable, String contentHash) {
        return new ProjectDescriptorView(
                descriptor.getName(),
                descriptor.getComment(),
                mapModules(descriptor.getModules()),
                mapDependencies(descriptor.getDependencies()),
                copyOf(descriptor.getClasspath()),
                mapOpenApi(descriptor.getOpenapi()),
                mapExposedMethods(descriptor.getExposedMethods()),
                descriptor.getPropertiesFileNameProcessor(),
                arrayToList(descriptor.getPropertiesFileNamePatterns()),
                editable,
                contentHash);
    }

    private static List<ModuleView> mapModules(@Nullable List<Module> modules) {
        if (modules == null) {
            return List.of();
        }
        List<ModuleView> result = new ArrayList<>(modules.size());
        for (Module module : modules) {
            var config = module.getWebstudioConfiguration();
            result.add(new ModuleView(
                    module.getName(),
                    module.getRulesRootPath(),
                    mapMethodFilter(module.getMethodFilter()),
                    config != null && config.isCompileThisModuleOnly(),
                    module.isModuleWithWildcard()));
        }
        return result;
    }

    private static @Nullable MethodFilterView mapMethodFilter(@Nullable MethodFilter filter) {
        if (filter == null) {
            return null;
        }
        return new MethodFilterView(copyOf(filter.getIncludes()), copyOf(filter.getExcludes()));
    }

    private static List<DependencyView> mapDependencies(@Nullable List<ProjectDependencyDescriptor> dependencies) {
        if (dependencies == null) {
            return List.of();
        }
        List<DependencyView> result = new ArrayList<>(dependencies.size());
        for (ProjectDependencyDescriptor dependency : dependencies) {
            result.add(new DependencyView(dependency.getName(), dependency.isAutoIncluded(), dependency.getMavenArtifact()));
        }
        return result;
    }

    private static @Nullable OpenApiView mapOpenApi(@Nullable OpenAPI openapi) {
        if (openapi == null) {
            return null;
        }
        return new OpenApiView(
                openapi.getPath(),
                openapi.getMode() == null ? null : openapi.getMode().name(),
                openapi.getModelModuleName(),
                openapi.getAlgorithmModuleName());
    }

    private static List<ExposedMethodView> mapExposedMethods(@Nullable ExposedMethods methods) {
        if (methods == null) {
            return List.of();
        }
        var result = new ArrayList<ExposedMethodView>();
        copyOf(methods.getIncludes()).forEach(pattern -> result.add(new ExposedMethodView(pattern, "include")));
        copyOf(methods.getExcludes()).forEach(pattern -> result.add(new ExposedMethodView(pattern, "exclude")));
        return result;
    }

    private static List<String> copyOf(@Nullable List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private static List<String> copyOf(@Nullable Set<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private static List<String> arrayToList(String @Nullable [] values) {
        return values == null ? List.of() : Arrays.stream(values).filter(Objects::nonNull).toList();
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
