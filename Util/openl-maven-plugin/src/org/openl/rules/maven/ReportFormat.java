package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;

import org.openl.rules.testmethod.TestUnitsResults;

// The constant names are the values of the reportsFormat parameter that project POMs spell out.
@SuppressWarnings("java:S115")
public enum ReportFormat {
    junit4,
    xlsx;

    void write(File dir, String suiteName, TestUnitsResults result) throws IOException, XMLStreamException {
        switch (this) {
            case xlsx -> new XlsxReportWriter(dir).write(suiteName, result);
            case junit4 -> new JUnitReportWriter(dir).write(suiteName, result);
            default -> throw new IllegalArgumentException(this + " writer is not found.");
        }
    }
}
