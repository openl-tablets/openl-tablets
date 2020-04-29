package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebStudioValidationUtils {

    private static final Logger log = LoggerFactory.getLogger(WebStudioValidationUtils.class);

    /**
     * Validates directory for write access. If specified folder is not writable the validation error will appears
     */
    public static void directoryValidator(Object value, String directoryType) {
        String dirPath;
        File directory;

        if (StringUtils.isNotEmpty((String) value)) {
            dirPath = (String) value;
            directory = new File(dirPath);

            if (directory.exists()) {
                if (directory.isDirectory()) {

                    if (directory.canWrite()) {
                        /*
                         * If canWrite() returns true the temp file will be created. It's needed because in Windows OS
                         * method canWrite() returns true if folder is not marked 'read only' but such folders can have
                         * security permissions 'deny all'
                         */
                        validateIsWritable(directory);
                    } else {
                        WebStudioUtils.throwValidationError(String.format(
                            "There is not enough access rights for installing '%s' into the folder: '%s'.",
                            directoryType,
                            dirPath));
                    }
                } else {
                    WebStudioUtils.throwValidationError(String.format("'%s' is not a folder.", dirPath));
                }
            } else {
                File parentFolder = directory.getAbsoluteFile().getParentFile();
                File existingFolder = null;

                while (parentFolder != null) {
                    if (parentFolder.exists()) {
                        existingFolder = parentFolder.getAbsoluteFile();

                        break;
                    }
                    parentFolder = parentFolder.getParentFile();
                }
                boolean hasAccess = directory.mkdirs();

                // for some cases mkdirs can return true without creating directory even if directory does not exist.
                // ex.: path/NUL (path folder should exists)
                if (!hasAccess || !directory.exists()) {
                    validateIsWritable(directory);
                } else {
                    deleteFolder(existingFolder, directory);
                }
            }

        } else {
            WebStudioUtils.throwValidationError(String.format("'%s' cannot be blank", directoryType));
        }
    }

    /**
     * Creates a temp file for validating folder write permissions
     *
     * @param file is a folder where temp file will be created
     */
    private static void validateIsWritable(File file) {

        try {
            File tmpFile = File.createTempFile("temp", null, file);
            if (!tmpFile.delete()) {
                log.warn("Cannot delete temp file {}.", tmpFile.getName());
            }

        } catch (IOException ioe) {
            WebStudioUtils.throwValidationError(String.format("%s for '%s'", ioe.getMessage(), file.getName()));
        }
    }

    /**
     * Deletes the folder which was created for validating folder permissions
     *
     * @param existingFolder folder which already exists on file system
     * @param studioFolder folder were studio will be installed
     */
    private static void deleteFolder(File existingFolder, File studioFolder) {
        if (studioFolder.exists() && !studioFolder.delete()) {
            log.warn("Cannot delete the folder {}.", studioFolder.getName());
        }

        if (existingFolder == null) {
            return;
        }

        while (!studioFolder.getAbsolutePath().equalsIgnoreCase(existingFolder.getAbsolutePath())) {
            if (studioFolder.exists() && !studioFolder.delete()) {
                log.warn("Cannot delete the folder {}.", studioFolder.getName());
            }
            studioFolder = studioFolder.getAbsoluteFile().getParentFile();
        }
    }
}
