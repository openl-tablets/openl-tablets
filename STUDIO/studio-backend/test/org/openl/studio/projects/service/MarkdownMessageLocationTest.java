package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.studio.projects.model.project.status.TableMessageSource;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Messages of a module written as Markdown files point at the cell of the table they are about, and at the piece
 * of its text, as for a workbook.
 */
class MarkdownMessageLocationTest {

    @TempDir
    Path project;

    @Test
    void aMessageOfAMarkdownTablePointsAtItsCellAndThePieceOfItsText() throws IOException {
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Errors</name>
                    <modules>
                        <module>
                            <name>Errors</name>
                            <rules-root path="rules/Errors"/>
                        </module>
                    </modules>
                </project>
                """);
        write("Model/Person.md", """
                ---
                table: Datatype Person
                ---

                | Type    | Name |
                |---------|------|
                | String  | name |
                | Integer | age  |
                """);
        write("surcharge.md", """
                ---
                table: SmartRules Double surcharge(Integer age)
                state: XX
                ---

                | Age   | Surcharge |
                |-------|-----------|
                | < 25  | 100       |
                | >= 25 | 0         |
                """);
        write("people.md", """
                ---
                table: Data Person people
                ---

                | name | age    |
                |------|--------|
                | Name | Age    |
                | Ann  | 30     |
                | Bob  | thirty |
                """);
        write("grade.md", """
                ---
                table: Rules String grade(Integer score)
                ---

                | C1          | RET1          |
                |-------------|---------------|
                | score >= mn | result        |
                | Integer min | String result |
                | Min Score   | Grade         |
                | 90          | A             |
                """);
        write("total.md", """
                ---
                table: Spreadsheet SpreadsheetResult total(Double premium)
                ---

                | Step  | Value                |
                |-------|----------------------|
                | Base  | `= premium`          |
                | Total | `= $Base - $Discont` |
                """);
        write("discount.md", """
                ---
                table: Method Double discount(Double premium)
                ---

                ```java
                double rate = premium > 1000 ? 0.1 : 0.05;
                return premium * rte;
                ```
                """);
        var model = TableTestProjects.projectModel(project);

        var mapper = new DetailedMessageDescriptionMapperImpl();
        var located = mapper.mapSorted(model.getModuleMessages(), model).stream()
                .map(message -> (TableMessageSource) message.location())
                .collect(Collectors.toMap(TableMessageSource::name,
                        at -> at.cell() + " " + at.start() + "-" + at.end()));

        // The cells of the virtual sheet of a file: the header in row 1, the properties under it, then the body
        assertEquals(Map.of(
                "surcharge", "C2 0-2",
                "people", "B5 0-6",
                "grade", "A3 9-11",
                "total", "B4 10-18",
                "discount", "A2 60-63"), located);
    }

    private void write(String file, String text) throws IOException {
        var path = project.resolve("rules/Errors").resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }
}
