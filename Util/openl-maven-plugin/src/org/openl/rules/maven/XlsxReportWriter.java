package org.openl.rules.maven;

import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.export.TestResultExport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class XlsxReportWriter {
    private final File dir;

    XlsxReportWriter(File dir) {
        this.dir = dir;
    }

    public void write(TestUnitsResults result) throws Exception {
        TestSuite testSuite = result.getTestSuite();
        String testName = testSuite.getTestSuiteMethod().getName();
        String moduleName = testSuite.getTestSuiteMethod().getModuleName();

        String suitName = "OpenL." + moduleName + "." + testName;
        String filename = "TEST-" + suitName + ".xlsx";

        if (!dir.mkdirs() && !dir.exists()) {
            throw new IOException(String.format("Cannot create folder '%s'.", dir.getAbsolutePath()));
        }

        File file = new File(dir, filename);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            new TestResultExport().export(outputStream, -1, result);
        }
    }
}
