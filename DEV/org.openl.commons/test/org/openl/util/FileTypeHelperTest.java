package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The workbooks the engine reads and the lock files it does not.
 *
 * @author Yury Molchan
 */
class FileTypeHelperTest {

    @ParameterizedTest
    @ValueSource(strings = {"Rules.xlsx", "Rules.XLS", "rules/Rules.xlsm"})
    void readsTheThreeExcelFormats(String name) {
        assertTrue(FileTypeHelper.isExcelFile(name));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"~$Rules.xlsx", "rules/~$Rules.xlsx", "rules\\~$Rules.xlsm", "Rules.txt", "Rules.xlsx.bak"})
    void readsNoWorkbookOutOfALockFileOrAnotherFile(String name) {
        assertFalse(FileTypeHelper.isExcelFile(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"~$Rules.xlsx", "~$Notes.docx"})
    void tellsTheLockFilesOfMicrosoftOffice(String name) {
        assertTrue(FileTypeHelper.isLockFile(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Rules.xlsx", "rules/~$Rules.xlsx", "$~Rules.xlsx"})
    void tellsNoOtherFileALockFile(String name) {
        assertFalse(FileTypeHelper.isLockFile(name));
    }
}
