package org.openl.rules.ui;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLMessage;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.validation.LocalizationCheck;
import org.openl.rules.validation.MessageCallCheck;

/**
 * The localization of the project a model compiles: the warnings about its localization files and about the calls of
 * {@code msg} its modules make.
 *
 * <p>The files are checked before any module is compiled, as reading them needs no compilation, so a compilation
 * tells about them first. The calls are checked as each module is compiled, against the messages of the project, its
 * dependencies and its libraries.
 *
 * <p>A write of the localization files is checked again without compiling anything: the files are read once more,
 * and the calls the compiled modules kept are checked against them.
 *
 * <p>The calls are checked one check at a time, each over every module checked so far, so the warnings about a module
 * are not lost when two modules finish compiling at once.
 */
@Slf4j
public final class ProjectLocalization {

    /**
     * What was found about the project.
     *
     * @param folder the project folder the files were read from
     * @param loader the class loader the messages are read through, closed once the state is replaced
     * @param source the messages a rule of the project reads
     * @param check   what the check of the files found
     * @param checked the modules whose calls were checked, by the loader that compiles each
     */
    private record State(Path folder,
                         URLClassLoader loader,
                         OpenLMessageSource source,
                         LocalizationCheck.Result check,
                         Map<IDependencyLoader, Checked> checked) {

        State withChecked(Map<IDependencyLoader, Checked> checked) {
            return new State(folder, loader, source, check, checked);
        }

        /** The warnings about the calls of every module checked. */
        List<OpenLMessage> calls() {
            return checked.values().stream().flatMap(module -> module.calls().stream()).toList();
        }
    }

    /**
     * A module as it was compiled, and the warnings about its calls.
     *
     * @param module the module, as the last compilation of it built it
     * @param calls  the warnings about its calls, or {@code null} when they are to be checked again
     */
    private record Checked(XlsModuleOpenClass module, @Nullable List<OpenLMessage> calls) {
    }

    private final AtomicReference<@Nullable State> state = new AtomicReference<>();

    /**
     * Checks the localization files of a project once, without compiling anything and keeping nothing.
     *
     * @param project  the project to check
     * @param projects the projects of the workspace: those the project depends on give their messages
     * @return what the check found
     */
    public static LocalizationCheck.Result check(ProjectDescriptor project, Collection<ProjectDescriptor> projects) {
        try (var loader = new URLClassLoader(classPathOf(project, projects), null)) {
            return LocalizationCheck.check(project.getProjectFolder(), new OpenLMessageSource(loader));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the localization files of " + project.getName(), e);
        }
    }

    /**
     * Checks the localization files of the project, and forgets what was found about the calls before. The modules of
     * the same project checked before are kept, so the next check of the calls checks them against the new files.
     *
     * @param project  the project compiled
     * @param projects the projects the compilation loads: those the project depends on give their messages
     */
    void checkFiles(ProjectDescriptor project, Collection<ProjectDescriptor> projects) {
        var folder = project.getProjectFolder();
        var loader = new URLClassLoader(classPathOf(project, projects), null);
        var source = new OpenLMessageSource(loader);
        var check = LocalizationCheck.check(folder, source);
        var previous = state.get();
        var kept = new LinkedHashMap<IDependencyLoader, Checked>();
        if (previous != null && previous.folder().equals(folder)) {
            previous.checked().forEach((by, checked) -> kept.put(by, new Checked(checked.module(), null)));
        }
        replace(new State(folder, loader, source, check, kept));
    }

    /**
     * Checks the calls of the modules compiled, each by the loader that compiles it, against the messages as they are
     * now.
     *
     * <p>A module compiled again replaces the one its loader compiled before, and only a module not checked yet, or
     * compiled again, is checked: the warnings of the others stay. A module whose loader is gone, as the module left
     * the project, is dropped with its warnings. After a check of the files every module is checked again.
     *
     * @param modules the modules compiled so far, by their loaders
     * @param loaders every loader the compilation has now
     */
    @Synchronized
    void checkCalls(Map<IDependencyLoader, XlsModuleOpenClass> modules, Collection<IDependencyLoader> loaders) {
        var current = state.get();
        while (current != null) {
            var checked = new LinkedHashMap<>(current.checked());
            checked.keySet().retainAll(Set.copyOf(loaders));
            modules.forEach((by, module) -> {
                var known = checked.get(by);
                if (known == null || known.module() != module) {
                    checked.put(by, new Checked(module, null));
                }
            });
            var source = current.source();
            checked.replaceAll((by, known) -> known.calls() != null ? known
                    : new Checked(known.module(), MessageCallCheck.checkModule(known.module(), source)));
            if (state.compareAndSet(current, current.withChecked(Collections.unmodifiableMap(checked)))) {
                return;
            }
            // The files were checked again meanwhile: the calls are checked against them.
            current = state.get();
        }
    }

    /**
     * Adds to the status what was found: the step of checking the files, counted when the project has them, and
     * the warnings.
     */
    void addTo(ProjectCompilationStatus.Builder status) {
        var current = state.get();
        if (current == null) {
            return;
        }
        if (current.check().files()) {
            status.addModulesCount(1).addModulesCompiled(1);
        }
        status.addMessages(current.check().messages()).addMessages(current.calls());
    }

    /**
     * The warnings about the calls of {@code msg} of the modules given. They are about the cells the calls are written
     * in, so they belong to the tables as much as the warnings of the compilation do.
     *
     * @param modules the modules whose warnings are asked for
     */
    List<OpenLMessage> callMessages(Collection<XlsModuleOpenClass> modules) {
        var current = state.get();
        if (current == null) {
            return List.of();
        }
        var asked = Collections.newSetFromMap(new IdentityHashMap<XlsModuleOpenClass, Boolean>());
        asked.addAll(modules);
        return current.checked()
                .values()
                .stream()
                .filter(checked -> asked.contains(checked.module()))
                .flatMap(checked -> checked.calls().stream())
                .toList();
    }

    /** Whether what was found is about the project in the folder. */
    boolean isOf(Path folder) {
        var current = state.get();
        return current != null && current.folder().equals(folder);
    }

    /** Forgets what was found, when the model lets go of the project. */
    void clear() {
        replace(null);
    }

    private void replace(@Nullable State next) {
        var previous = state.getAndSet(next);
        if (previous != null) {
            try {
                previous.loader().close();
            } catch (IOException e) {
                log.debug("Failed to close the class loader of the localization files", e);
            }
        }
    }

    /**
     * The class path the messages of the project are read through: the project first, then every project it depends
     * on, directly or through others, each with its libraries.
     */
    private static URL[] classPathOf(ProjectDescriptor project, Collection<ProjectDescriptor> workspace) {
        var urls = new ArrayList<URL>(Arrays.asList(project.getClassPathUrls()));
        dependenciesOf(project, workspace)
                .forEach(dependency -> urls.addAll(Arrays.asList(dependency.getClassPathUrls())));
        return urls.toArray(URL[]::new);
    }

    /**
     * The projects a project depends on, directly or through others, the nearest first: the order a rule looks for a
     * message the project lacks in.
     *
     * @param project   the project
     * @param workspace the projects of the workspace; a dependency it lacks is left out
     * @return the projects, the project itself left out
     */
    public static List<ProjectDescriptor> dependenciesOf(ProjectDescriptor project,
                                                         Collection<ProjectDescriptor> workspace) {
        Map<String, ProjectDescriptor> projects = workspace.stream()
                .collect(Collectors.toMap(ProjectDescriptor::getName, Function.identity(), (one, other) -> one));
        var reached = new LinkedHashSet<ProjectDescriptor>();
        var pending = new ArrayDeque<ProjectDescriptor>();
        pending.add(project);
        while (!pending.isEmpty()) {
            var next = pending.poll();
            if (reached.add(next) && next.getDependencies() != null) {
                next.getDependencies()
                        .stream()
                        .map(ProjectDependencyDescriptor::getName)
                        .map(projects::get)
                        .filter(Objects::nonNull)
                        .forEach(pending::add);
            }
        }
        reached.remove(project);
        return List.copyOf(reached);
    }
}
