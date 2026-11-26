package org.openl.rules.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/** Projects the tests of {@link ProjectModel} write, and the waits on what the model compiles of them. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ProjectModelTestProjects {

    /** The module of the project with the given name. */
    static Module module(ProjectDescriptor project, String name) {
        return project.getModules()
                .stream()
                .filter(module -> module.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    /** Waits for the compilation the model runs now, however it ends. */
    static void awaitCompiled(ProjectModel model) {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }

    /** Writes a module of the tables given, each one below the other with an empty row between. */
    static void writeModule(Path project, String name, String[][]... tables) throws IOException {
        try (var book = new HSSFWorkbook()) {
            var sheet = book.createSheet(name);
            var rowIndex = 0;
            for (var table : tables) {
                for (var cells : table) {
                    var row = sheet.createRow(rowIndex++);
                    for (var column = 0; column < cells.length; column++) {
                        row.createCell(column).setCellValue(cells[column]);
                    }
                }
                rowIndex++;
            }
            try (OutputStream out = Files.newOutputStream(project.resolve(name + ".xls"))) {
                book.write(out);
            }
        }
    }
}
