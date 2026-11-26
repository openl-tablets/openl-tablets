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
 * and the calls the compiled modules kept are checked against them. The files and the calls are told together, so a
 * read never finds the calls checked against other files.
 *
 * <p>The checks run one at a time, each over every module checked so far, so no warnings about a module are lost when
 * two modules finish compiling at once or the files are written meanwhile. Reads never wait for a check.
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
     * @param calls  the warnings about its calls
     */
    private record Checked(XlsModuleOpenClass module, List<OpenLMessage> calls) {

        /** The module with its calls checked against the messages. */
        static Checked of(XlsModuleOpenClass module, OpenLMessageSource source) {
            return new Checked(module, MessageCallCheck.checkModule(module, source));
        }
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
     * Checks the localization files of the project, then the calls of its modules against them, and tells both at
     * once. The modules of the same project checked before are checked again, unless their loader is gone.
     *
     * @param project  the project compiled
     * @param projects the projects the compilation loads: those the project depends on give their messages
     * @param modules  the modules the loaders hold, read before the check: they add the modules not checked yet
     * @param loaders  every loader the compilation has now
     */
    @Synchronized
    void checkFiles(ProjectDescriptor project,
                    Collection<ProjectDescriptor> projects,
                    Map<IDependencyLoader, XlsModuleOpenClass> modules,
                    Collection<IDependencyLoader> loaders) {
        var folder = project.getProjectFolder();
        var loader = new URLClassLoader(classPathOf(project, projects), null);
        var source = new OpenLMessageSource(loader);
        var check = LocalizationCheck.check(folder, source);
        var previous = state.get();
        var checked = new LinkedHashMap<IDependencyLoader, Checked>();
        if (previous != null && previous.folder().equals(folder)) {
            checked.putAll(previous.checked());
        }
        replace(new State(folder, loader, source, check, fill(checked, modules, loaders, source, true)));
    }

    /**
     * Checks the calls of a module compiled a moment ago against the messages as they are now, with those of the
     * modules compiled before it and not checked yet.
     *
     * <p>The module replaces the one its loader compiled before. A loader holds a module only after it tells about
     * it, so the modules the loaders hold may be older than those checked already: they add only the modules not
     * checked yet, and the warnings of the others stay.
     *
     * @param compiler the loader that compiled the module
     * @param module   the module compiled
     * @param modules  the modules the loaders hold, read before the check
     * @param loaders  every loader the compilation has now
     */
    @Synchronized
    void checkCalls(IDependencyLoader compiler,
                    XlsModuleOpenClass module,
                    Map<IDependencyLoader, XlsModuleOpenClass> modules,
                    Collection<IDependencyLoader> loaders) {
        var current = state.get();
        if (current != null) {
            var source = current.source();
            var checked = new LinkedHashMap<>(current.checked());
            checked.compute(compiler,
                    (by, before) -> before != null && before.module() == module ? before : Checked.of(module, source));
            state.set(current.withChecked(fill(checked, modules, loaders, source, false)));
        }
    }

    /**
     * Completes the modules checked, by their loaders: a module whose loader is gone, as the module left the project,
     * is dropped with its warnings, and a module not checked yet is checked.
     *
     * @param checked the modules checked so far, completed in place
     * @param again   whether the modules checked so far are checked again, as the messages changed
     * @return the modules checked
     */
    private static Map<IDependencyLoader, Checked> fill(Map<IDependencyLoader, Checked> checked,
                                                        Map<IDependencyLoader, XlsModuleOpenClass> modules,
                                                        Collection<IDependencyLoader> loaders,
                                                        OpenLMessageSource source,
                                                        boolean again) {
        checked.keySet().retainAll(Set.copyOf(loaders));
        if (again) {
            checked.replaceAll((by, before) -> Checked.of(before.module(), source));
        }
        modules.forEach((by, module) -> checked.computeIfAbsent(by, any -> Checked.of(module, source)));
        return checked;
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
    @Synchronized
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
