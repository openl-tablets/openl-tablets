package org.openl.rules.lang.xls;

import java.io.File;
import java.io.IOException;

import lombok.Getter;

/**
 * A workbook is not saved because its file changed on disk since the workbook was read from it.
 *
 * <p>Another program, or another reader of the same file, wrote it in the meantime. Saving the workbook would
 * overwrite that change, so the file is left as it is. The workbook has to be read again first.
 */
@Getter
public class WorkbookChangedOutsideException extends IOException {

    private final transient File file;

    public WorkbookChangedOutsideException(File file) {
        super("The file '%s' changed on disk since it was read.".formatted(file.getName()));
        this.file = file;
    }
}
