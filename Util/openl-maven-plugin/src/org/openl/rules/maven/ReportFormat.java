package org.openl.rules.maven;

import java.io.File;

import org.openl.rules.testmethod.TestUnitsResults;

public enum ReportFormat {
    junit4,
    xlsx;

    void write(File dir, TestUnitsResults result) throws Exception {
        switch (this) {
            case xlsx:
                new XlsxReportWriter(dir).write(result);
                return;
            case junit4:
                new JUnitReportWriter(dir).write(result);
                return;
        }
        throw new IllegalArgumentException(this + " writer is not found.");
    }
}
