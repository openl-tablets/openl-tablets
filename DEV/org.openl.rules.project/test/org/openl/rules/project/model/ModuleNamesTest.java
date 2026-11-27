package org.openl.rules.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleNamesTest {

    private static final String NO_MODULES = "<project><name>p</name></project>";

    @TempDir
    Path project;

    @Test
    void namesWorkbooksOfOneNameAfterTheirFolders() throws IOException {
        files("rules/FL/calc.xlsx", "rules/NY/calc.xlsx", "rules/Main.xlsx");

        assertEquals(List.of("calc (FL)=rules/FL/calc.xlsx", "Main=rules/Main.xlsx", "calc (NY)=rules/NY/calc.xlsx"),
                modules(NO_MODULES));
        assertEquals(List.of(), ModuleNames.duplicates(expand(NO_MODULES).getModules()));
    }

    @Test
    void takesAsManyFoldersAsTellTheModulesApart() throws IOException {
        files("rules/a/x/calc.xlsx", "rules/b/x/calc.xlsx", "rules/b/y/calc.xlsx");

        assertEquals(List.of("calc (a - x)=rules/a/x/calc.xlsx",
                "calc (b - x)=rules/b/x/calc.xlsx",
                "calc (b - y)=rules/b/y/calc.xlsx"), modules(NO_MODULES));
    }

    /** A workbook already named as the first folder would name another module takes more folders. */
    @Test
    void givesNoNameAnotherModuleHas() throws IOException {
        files("rules/FL/calc.xlsx", "rules/NY/calc.xlsx", "rules/calc (FL).xlsx");

        assertEquals(List.of("calc (rules - FL)=rules/FL/calc.xlsx",
                "calc (rules - NY)=rules/NY/calc.xlsx",
                "calc (FL)=rules/calc (FL).xlsx"), modules(NO_MODULES));
    }

    @Test
    void readsNoFolderOfADotOrATrailingSlash() throws IOException {
        files("calc.xlsx", "FL/calc.xlsx", "rules/FL/Rates/rate.md", "rules/NY/Rates/rate.md");

        assertEquals(List.of("calc=./calc.xlsx",
                        "calc (FL)=FL/calc.xlsx",
                        "Rates (FL)=rules/FL/Rates/",
                        "Rates (NY)=rules/NY/Rates/"),
                modules(descriptor(module(null, "./calc.xlsx"),
                        module(null, "FL/calc.xlsx"),
                        module(null, "rules/FL/Rates/"),
                        module(null, "rules/NY/Rates/"))));
    }

    @Test
    void namesFoldersOfMarkdownTablesOfOneNameAfterTheirFolders() throws IOException {
        files("rules/FL/Rates/rate.md", "rules/NY/Rates/rate.md");

        assertEquals(List.of("Rates (FL)=rules/FL/Rates", "Rates (NY)=rules/NY/Rates"),
                modules(descriptor(module(null, "rules/*/*"))));
    }

    @Test
    void keepsTheNameOfAWorkbookInTheProjectRoot() throws IOException {
        files("calc.xlsx", "FL/calc.xlsx");

        assertEquals(List.of("calc (FL)=FL/calc.xlsx", "calc=calc.xlsx"),
                modules(descriptor(module(null, "**/calc.xlsx"))));
    }

    @Test
    void keepsTheNameDeclaredInRulesXml() throws IOException {
        files("FL/calc.xlsx", "NY/calc.xlsx");

        assertEquals(List.of("calc=FL/calc.xlsx", "calc (NY)=NY/calc.xlsx"),
                modules(descriptor(module("calc", "FL/calc.xlsx"), module(null, "*/calc.xlsx"))));
    }

    @Test
    void namesModulesDeclaredWithoutANameAfterTheirFolders() throws IOException {
        files("FL/calc.xlsx", "NY/calc.xlsx");

        assertEquals(List.of("calc (FL)=FL/calc.xlsx", "calc (NY)=NY/calc.xlsx"),
                modules(descriptor(module(null, "FL/calc.xlsx"), module(null, "NY/calc.xlsx"))));
    }

    @Test
    void reportsModulesDeclaredUnderOneName() throws IOException {
        files("FL/calc.xlsx", "NY/calc.xlsx", "Main.xlsx");
        var descriptor = descriptor(module("calc", "FL/calc.xlsx"),
                module("calc", "NY/calc.xlsx"),
                module("Main", "Main.xlsx"));

        assertEquals(List.of("calc=FL/calc.xlsx", "calc=NY/calc.xlsx", "Main=Main.xlsx"), modules(descriptor));
        assertEquals(List.of("Several modules are named 'calc': 'FL/calc.xlsx', 'NY/calc.xlsx'."
                        + " Each module of the project must have a name of its own."),
                ModuleNames.duplicates(expand(descriptor).getModules()));
    }

    /** A workbook in the project root has no folder to tell it apart from a module declared under its name. */
    @Test
    void reportsAWorkbookNoFolderTellsApart() throws IOException {
        files("calc.xlsx", "FL/calc.xlsx");
        var descriptor = descriptor(module("calc", "FL/calc.xlsx"), module(null, "**/calc.xlsx"));

        assertEquals(List.of("calc=FL/calc.xlsx", "calc=calc.xlsx"), modules(descriptor));
        assertEquals(List.of("Several modules are named 'calc': 'FL/calc.xlsx', 'calc.xlsx'."
                        + " Each module of the project must have a name of its own."),
                ModuleNames.duplicates(expand(descriptor).getModules()));
    }

    private void files(String... paths) throws IOException {
        for (var path : paths) {
            var file = project.resolve(path);
            Files.createDirectories(file.getParent());
            var content = path.endsWith(".md") ? "---\ntable: Datatype Rate\n---\n" : "";
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
    }

    private static String module(@Nullable String name, String path) {
        var named = name == null ? "" : "<name>" + name + "</name>";
        return "<module>" + named + "<rules-root path=\"" + path + "\"/></module>";
    }

    private static String descriptor(String... modules) {
        return "<project><name>p</name><modules>" + String.join("", modules) + "</modules></project>";
    }

    private List<String> modules(String descriptor) throws IOException {
        return expand(descriptor).getModules()
                .stream()
                .map(module -> module.getName() + "=" + module.getRulesRootPath())
                .toList();
    }

    private ProjectDescriptor expand(String descriptor) throws IOException {
        Files.writeString(project.resolve(ProjectDescriptor.FILE_NAME), descriptor, StandardCharsets.UTF_8);
        return Objects.requireNonNull(ProjectDescriptor.read(project)).expand();
    }
}
