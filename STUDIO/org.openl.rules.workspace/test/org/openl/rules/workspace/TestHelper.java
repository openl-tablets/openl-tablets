package org.openl.rules.workspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Ignore;

@Ignore("Manual test")
public class TestHelper {
    /**
     * Working folder for tests.
     */
    public static String FOLDER_TEST = "test_work";

    private static final WorkspaceUser USER_TEST = new WorkspaceUserImpl("test");

    public static boolean clearDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        for (File file : dir.listFiles()) {
            if (!deleteFile(file)) {
                return false;
            }
        }
        return true;
    }

    public static File createEmptyFile(File folder, String filename) throws IOException {
        File file = new File(folder, filename);
        FileOutputStream fos = new FileOutputStream(file);
        fos.close();
        return file;
    }

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!deleteFile(f)) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    public static void deleteTestFolder() {
        deleteFile(new File(FOLDER_TEST));
    }

    /**
     * After running this method {@link #FOLDER_TEST} exists inside of
     * <i>current directory</i> and is empty.
     *
     * @throws IOException if operation fails
     */
    public static void ensureTestFolderExistsAndClear() throws IOException {
        File testFolder = new File(FOLDER_TEST);
        if (!testFolder.isDirectory()) {
            testFolder.mkdirs();
        }

        if (!testFolder.isDirectory()) {
            throw new IOException("failed to create directory: " + FOLDER_TEST);
        }

        if (!clearDirectory(testFolder)) {
            throw new IOException("failed to clear directory: " + FOLDER_TEST);
        }
    }

    public static WorkspaceUser getWorkspaceUser() {
        return USER_TEST;
    }
}
