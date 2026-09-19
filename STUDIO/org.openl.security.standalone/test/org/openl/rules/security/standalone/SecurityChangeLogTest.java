package org.openl.rules.security.standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Guards what holds the change logs together once they are split across files, and what no single change log
 * can check for itself.
 *
 * @author Yury Molchan
 */
class SecurityChangeLogTest {

    private static final String MASTER = "db.changelog-master.xml";

    /** The sources rather than the build output, which may still hold a change log of an earlier build. */
    private static final Path CHANGE_LOGS = Path.of("resources/db/changelog");

    /** Every change log of the module, named the way the master change log includes it. */
    static List<String> changeLogs() throws IOException {
        try (Stream<Path> files = Files.walk(CHANGE_LOGS)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> CHANGE_LOGS.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    private static String read(String changeLog) throws IOException {
        return Files.readString(CHANGE_LOGS.resolve(changeLog));
    }

    /**
     * An included change log does not inherit the quoting strategy of the one that includes it. Without the
     * attribute of its own, PostgreSQL would create "OpenL_Tag_Types", an object an upgraded database does not
     * have and the application cannot address.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("changeLogs")
    void quotesNoIdentifier(String changeLog) throws IOException {
        assertTrue(read(changeLog).contains("objectQuotingStrategy=\"QUOTE_ONLY_RESERVED_WORDS\""),
                changeLog + " must declare the quoting strategy on its root element");
    }

    /** A change log the master does not include is applied to no database at all. */
    @Test
    void includesEveryChangeLog() throws IOException {
        var master = read(MASTER);
        var missing = changeLogs().stream()
                .filter(changeLog -> !changeLog.equals(MASTER))
                .filter(changeLog -> !master.contains("file=\"" + changeLog + "\""))
                .toList();

        assertEquals(List.of(), missing, "db.changelog-master.xml must include every change log");
    }

    /**
     * A change set of {@code install/} describes the tables of the current release, so a later release edits
     * it instead of adding to it. It can only ever run on a database that has none of those tables, which is
     * why it accepts the checksum of such an edit rather than failing every installation that already carries
     * them.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("installChangeLogs")
    void acceptsAnEditOfTheTablesItCreates(String changeLog) throws IOException {
        var content = read(changeLog);

        assertEquals(count(content, "<changeSet "), count(content, "<validCheckSum>1:any</validCheckSum>"),
                changeLog + " must accept the checksum of an edit in every change set");
    }

    static List<String> installChangeLogs() throws IOException {
        return changeLogs().stream().filter(changeLog -> changeLog.startsWith("install/")).toList();
    }

    private static long count(String content, String element) {
        return content.lines().filter(line -> line.contains(element)).count();
    }
}
