package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;

class MessageCallCheckTest {

    /** The steps of the Spreadsheet the module writes, each a name and a formula. */
    private static final String[][] STEPS = {
            {"Fits", "= msg(\"greeting\", \"Ann\")"},
            {"TooMany", "= msg(\"greeting\", \"Ann\", \"Bob\")"},
            {"Missing", "= msg(\"missing.key\")"},
            {"AsWritten", "= msg(\"greeting\")"},
            {"NoValue", "= msg(\"farewell\", null)"},
            {"Built", "= msg(\"gree\" + \"ting\", 1, 2)"},
            {"Choice", "= msg(true ? \"greeting\" : \"absent\", \"x\")"},
    };

    @TempDir
    Path folder;

    @Test
    void checksEveryKeyWrittenAsTextAgainstTheDefaultTexts() throws IOException {
        Files.createDirectories(folder.resolve("i18n"));
        Files.writeString(folder.resolve("i18n/message.properties"), "greeting = Hello, {0}!\nfarewell = Bye {0}\n");
        var module = compile();

        List<OpenLMessage> messages;
        try (var loader = new URLClassLoader(new URL[] {folder.toUri().toURL()}, null)) {
            messages = MessageCallCheck.checkModule(module, new OpenLMessageSource(loader));
        }

        // A message given no values, or a single null, is returned as written, and a key built from parts is known
        // at runtime alone. Each key a choice may give is checked.
        assertEquals(List.of("'greeting' has 1 placeholder, but 2 values are passed.",
                "Key 'missing.key' has no default text: a language without a text of its own returns the key.",
                "Key 'absent' has no default text: a language without a text of its own returns the key."),
                messages.stream().map(OpenLMessage::getSummary).toList());
        // Each is about the cell the call is written in.
        messages.forEach(message -> assertNotNull(message.getSourceLocation()));
    }

    @Test
    void keepsTheCallsWhoseKeyIsWrittenAsText() throws IOException {
        var calls = compile().getMessageCalls();

        assertEquals(List.of("greeting 1", "greeting 2", "missing.key 0", "greeting 0", "farewell 0", "greeting 1",
                "absent 1"), calls.stream().map(call -> call.key() + " " + call.values()).toList());
    }

    @Test
    void checksNothingOfAModuleWithoutCalls() throws IOException {
        var module = mock(XlsModuleOpenClass.class);
        when(module.getMessageCalls()).thenReturn(List.of());
        try (var loader = new URLClassLoader(new URL[] {folder.toUri().toURL()}, null)) {
            assertTrue(MessageCallCheck.checkModule(module, new OpenLMessageSource(loader)).isEmpty());
        }
    }

    /** The module of one Spreadsheet whose steps call msg. */
    private XlsModuleOpenClass compile() throws IOException {
        var file = folder.resolve("Calls.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Calls");
            sheet.createRow(0).createCell(0).setCellValue("Spreadsheet SpreadsheetResult Calls()");
            var header = sheet.createRow(1);
            header.createCell(0).setCellValue("Step");
            header.createCell(1).setCellValue("Value");
            for (var i = 0; i < STEPS.length; i++) {
                var row = sheet.createRow(i + 2);
                row.createCell(0).setCellValue(STEPS[i][0]);
                row.createCell(1).setCellValue(STEPS[i][1]);
            }
            workbook.write(out);
        }
        var compiled = new RulesEngineFactory<>(file.toUri().toURL()).getCompiledOpenClass();
        return (XlsModuleOpenClass) compiled.getOpenClassWithErrors();
    }
}
