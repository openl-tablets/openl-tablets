package org.openl.rules.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.export.TestResultExport;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class XlsxReportWriter {
    private final File dir;

    public void write(String suiteName, TestUnitsResults result) throws IOException {
        var filename = "TEST-" + suiteName + ".xlsx";

        if (!dir.mkdirs() && !dir.exists()) {
            throw new IOException("Cannot create folder '%s'.".formatted(dir.getAbsolutePath()));
        }

        var file = new File(dir, filename);
        try (var outputStream = new FileOutputStream(file)) {
            new TestResultExport().export(outputStream, -1, result);
        }
    }
}
