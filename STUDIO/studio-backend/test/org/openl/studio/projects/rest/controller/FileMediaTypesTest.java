package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileMediaTypesTest {

    @ParameterizedTest
    @CsvSource({
            "README.md, text/markdown",
            "notes.MARKDOWN, text/markdown",
            "openapi.yaml, application/yaml",
            ".github/build.yml, application/yaml",
            "i18n/messages.properties, text/plain",
            "scripts/build.groovy, text/plain",
            "rules.xml, application/xml",
            "data.json, application/json",
            "notes.txt, text/plain",
            "rules/Model.xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "data.bin, application/octet-stream",
            "LICENSE, application/octet-stream"
    })
    void answersTheTypeOfAFileByItsExtension(String fileName, String type) {
        assertEquals(type, FileMediaTypes.of(fileName).toString());
    }
}
