package org.openl.rules.maven;

import static org.openl.rules.testmethod.TestStatus.TR_OK;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.types.impl.ThisField;

/**
 * Creates a xml report files compatible with surefire and JUnit formats.
 * See http://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd
 * See https://github.com/windyroad/JUnit-Schema/blob/master/JUnit.xsd
 *
 * Not thread-safe.
 */
class JUnitReportWriter {
    // NumberFormat is not thread-safe
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
    private final File dir;
    private XMLStreamWriter xml;

    JUnitReportWriter(File dir) {
        this.dir = dir;
    }

    private static final String CDATA_START = "<![CDATA[";
    private static final String CDATA_END = "]]>";

    private void writeCData(String data) throws XMLStreamException {
        xml.writeCData(data.replace(CDATA_END, "]]" + CDATA_END + CDATA_START + ">"));
    }

    private void newLine() throws XMLStreamException {
        text("\n");
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
    }

    private String getTime(long nanos) {
        return numberFormat.format((double) nanos / 1000_000_000);
    }

    private void attr(String name, String value) throws XMLStreamException {
        xml.writeAttribute(name, value);
    }

    private void start(String name) throws XMLStreamException {
        newLine();
        xml.writeStartElement(name);
    }

    private void end() throws XMLStreamException {
        newLine();
        xml.writeEndElement();
    }

    private void empty(String name) throws XMLStreamException {
        newLine();
        xml.writeEmptyElement(name);
    }

    private void text(String text) throws XMLStreamException {
        xml.writeCharacters(text);
    }

    void write(TestUnitsResults result) throws Exception {
        TestSuite testSuite = result.getTestSuite();
        String testName = testSuite.getTestSuiteMethod().getName();
        String moduleName = testSuite.getTestSuiteMethod().getModuleName();

        String suitName = "OpenL." + moduleName + "." + testName;
        String filename = "TEST-" + suitName + ".xml";

        int tests = result.getNumberOfTestUnits();
        int failures = result.getNumberOfAssertionFailures();
        int errors = result.getNumberOfErrors();
        long executionTime = result.getExecutionTime();
        List<ITestUnit> testUnits = result.getTestUnits();

        dir.mkdirs();
        File file = new File(dir, filename);
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        xml = factory.createXMLStreamWriter(writer);

        writeTestsuite(suitName, tests, failures, errors, executionTime, testUnits);

    }

    private void writeTestsuite(String name,
            int tests,
            int failures,
            int errors,
            long executionTime,
            List<ITestUnit> testUnits) throws XMLStreamException {

        xml.writeStartDocument("UTF-8", "1.0");
        start("testsuite");

        attr("name", name);
        attr("tests", String.valueOf(tests));
        attr("skipped", "0");
        attr("failures", String.valueOf(failures));
        attr("errors", String.valueOf(errors));
        attr("time", getTime(executionTime));
        attr("timestamp", getCurrentDateTime());

        for (ITestUnit test : testUnits) {
            writeTestcase(name, test);
        }

        end();
        newLine();

        xml.writeEndDocument();
        xml.flush();
        xml.close();
    }

    private void writeTestcase(String testName, ITestUnit test) throws XMLStreamException {

        if (test.getResultStatus() == TR_OK) {
            empty("testcase");
        } else {
            start("testcase");
        }

        attr("name", test.getTest().getId());
        attr("classname", testName);
        attr("time", getTime(test.getExecutionTime()));

        writeErrorOrFailureElement(test);

        if (test.getResultStatus() != TR_OK) {
            end();
        }
    }

    private void writeErrorOrFailureElement(ITestUnit test) throws XMLStreamException {

        TestStatus testStatus = test.getResultStatus();
        switch (testStatus) {
            case TR_OK:
                break;
            case TR_NEQ:
                start("failure");
                attr("type", "ComparisonFailure");
                writeCData(failureMessage(test));
                end();
                break;
            case TR_EXCEPTION:
                Throwable throwable = (Throwable) test.getActualResult();
                start("error");
                attr("type", throwable.getClass().getName());
                attr("message", throwable.getMessage());
                writeCData(ExceptionUtils.getStackTrace(throwable));
                end();
                break;
            default:
                throw new IllegalArgumentException("Unexpected TestStatus." + testStatus.name());
        }
    }

    private String failureMessage(ITestUnit testUnit) {
        StringBuilder summaryBuilder = new StringBuilder();
        List<ComparedResult> comparisonResults = testUnit.getComparisonResults();
        for (ComparedResult comparisonResult : comparisonResults) {
            if (comparisonResult.getStatus() != TR_OK) {
                summaryBuilder.append('\n');
                if (comparisonResult.getFieldName().equals(ThisField.THIS)) {

                    summaryBuilder.append("Expected: <")
                        .append(comparisonResult.getExpectedValue())
                        .append("> but was <")
                        .append(comparisonResult.getActualValue())
                        .append(">");
                } else {
                    summaryBuilder.append("Field ")
                        .append(comparisonResult.getFieldName())
                        .append(" expected: <")
                        .append(comparisonResult.getExpectedValue())
                        .append("> but was <")
                        .append(comparisonResult.getActualValue())
                        .append(">");
                }
            }
        }
        return summaryBuilder.toString();
    }

}
