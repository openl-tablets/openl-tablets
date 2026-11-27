package org.openl.rules.project.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Keeps the names of the modules of a project apart.
 *
 * <p>A module is known by its name across the project: an {@code Environment} table depends on it by name, and two
 * modules of one name are one module to the engine.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModuleNames {

    /** Joins the folders a module is told apart by, as {@code calc (FL - 2024)}. */
    private static final String FOLDER_SEPARATOR = " - ";

    private static final String SEVERAL_MODULES_OF_ONE_NAME =
            "Several modules are named '%s': %s. Each module of the project must have a name of its own.";

    /**
     * Names the modules the engine names apart from the other modules of the project.
     *
     * <p>The engine names a module that rules.xml gives no name of its own: a module declared without a name, and a
     * file or a folder a path pattern matches. It names such a module after its file. When another module has that
     * name too, the engine adds the last folders of the path, as few as tell the modules apart:
     * {@code rules/FL/calc.xlsx} and {@code rules/NY/calc.xlsx} are {@code calc (FL)} and {@code calc (NY)}.
     *
     * <p>A module named in rules.xml keeps its name. A module in the project root keeps the name of its file while
     * no module named in rules.xml has it. A name already taken by another module of the project is not given, so
     * modules that no folder tells apart keep their names.
     *
     * @param modules          the modules of the project
     * @param namedByTheEngine tells the modules the engine names
     */
    public static void nameApart(List<Module> modules, Predicate<Module> namedByTheEngine) {
        var groups = byName(modules);
        groups.values()
                .stream()
                .filter(sameName -> sameName.size() > 1)
                .forEach(sameName -> nameApartInGroup(sameName, namedByTheEngine, groups.keySet()));
    }

    /** The modules that have a name, by their names, in the order the project declares them. */
    private static Map<String, List<Module>> byName(Collection<Module> modules) {
        return modules.stream()
                .filter(module -> module.getName() != null)
                .collect(Collectors.groupingBy(Module::getName, LinkedHashMap::new, Collectors.toList()));
    }

    /** Names apart the modules of one name that the engine names, giving no name another module has. */
    private static void nameApartInGroup(List<Module> sameName,
                                         Predicate<Module> namedByTheEngine,
                                         Set<String> taken) {
        var name = sameName.getFirst().getName();
        var renamed = sameName.stream().filter(namedByTheEngine).toList();
        var plainNameTaken = renamed.size() < sameName.size();
        var folders = renamed.stream().map(ModuleNames::foldersOf).toList();
        var depth = folders.stream().mapToInt(List::size).max().orElse(0);
        for (var count = 1; count <= depth; count++) {
            var names = namesAt(name, folders, count);
            if (areApart(names, name, plainNameTaken, taken)) {
                for (var i = 0; i < renamed.size(); i++) {
                    renamed.get(i).setName(names.get(i));
                }
                return;
            }
        }
    }

    /** The names of the modules told apart by up to {@code count} last folders of their paths. */
    private static List<String> namesAt(String name, List<List<String>> folders, int count) {
        return folders.stream().map(path -> {
            var tail = path.subList(Math.max(0, path.size() - count), path.size());
            return tail.isEmpty() ? name : name + " (" + String.join(FOLDER_SEPARATOR, tail) + ")";
        }).toList();
    }

    /**
     * Whether the names differ from each other and from the names of the other modules of the project. The plain
     * name stays free only for a module of the group when no module named in rules.xml has it.
     */
    private static boolean areApart(List<String> names, String name, boolean plainNameTaken, Set<String> taken) {
        return new HashSet<>(names).size() == names.size() && names.stream()
                .noneMatch(candidate -> candidate.equals(name) ? plainNameTaken : taken.contains(candidate));
    }

    /**
     * The folders of the path of a module, from the project root to the folder holding the module. A {@code .}
     * folder and a {@code /} at the end, as in {@code ./calc.xlsx} or {@code rules/FL/Rates/}, are no folders.
     */
    private static List<String> foldersOf(Module module) {
        var folders = Arrays.stream(module.getRulesRootPath().split("/"))
                .filter(folder -> !folder.isEmpty() && !".".equals(folder))
                .toList();
        return folders.isEmpty() ? folders : folders.subList(0, folders.size() - 1);
    }

    /**
     * Tells which names several modules of the project have.
     *
     * <p>Each message names the modules by their paths, in the order the project declares them.
     *
     * @param modules the modules of the project
     * @return a message for each name of several modules, none when every module has a name of its own
     */
    public static List<String> duplicates(Collection<Module> modules) {
        return byName(modules).entrySet()
                .stream()
                .filter(sameName -> sameName.getValue().size() > 1)
                .map(sameName -> SEVERAL_MODULES_OF_ONE_NAME.formatted(sameName.getKey(), pathsOf(sameName.getValue())))
                .toList();
    }

    /** The paths of the modules, quoted, as a message lists them. */
    private static String pathsOf(List<Module> modules) {
        return modules.stream()
                .map(module -> "'" + module.getRulesRootPath() + "'")
                .collect(Collectors.joining(", "));
    }
}
