/*
 * Created on Dec 15, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.webstudio.util;

import java.awt.Desktop;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MS Excel launcher.
 *
 * @author sam
 */
public class ExcelLauncher {
    private static final int LOCK_DETECT_TIMEOUT = 20000;

    private Logger log = LoggerFactory.getLogger(ExcelLauncher.class);

    private String scriptPath;

    private String workbookPath;
    private String workbookName;
    private String worksheetName;
    private String range;

    public ExcelLauncher(String scriptPath, String wbPath, String wbName, String wsName, String range) {
        this.scriptPath = scriptPath;
        this.workbookPath = wbPath;
        this.workbookName = wbName;
        this.worksheetName = wsName;
        this.range = range;
    }

    public void launch() throws Exception {
        if (range == null || range.equals("null")) {
            range = "A1";
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String[] cmdarray = { "wscript",
                    scriptPath,
                    workbookPath,
                    workbookName,
                    worksheetName == null ? "1" : worksheetName,
                    range, };

            fixEnvironmentForExcel();
            Process process = Runtime.getRuntime().exec(cmdarray);
            Thread lockDetector = createLockDetectorThread(process);

            lockDetector.start();
            process.waitFor();
            lockDetector.interrupt();
        } else {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(new File(workbookPath, workbookName));
            } else {
                throw new UnsupportedOperationException("Unsupported OS");
            }
        }
    }

    public static void launch(String scriptPath,
            String wbPath,
            String wbName,
            String wsName,
            String range) throws Exception {
        new ExcelLauncher(scriptPath, wbPath, wbName, wsName, range).launch();
    }

    /**
     * Unfortunately this is a hack. See this link for details:
     * http://www.codeproject.com/Questions/161160/Using-windows-service-to-open-an-Excel-file
     */
    private void fixEnvironmentForExcel() {
        String winDir = System.getenv("windir");
        String x64Path = winDir + "\\SysWow64\\Config\\SystemProfile";
        String x32Path = winDir + "\\System32\\Config\\SystemProfile";
        if (absentDesktop(x64Path)) {
            new File(x64Path, "Desktop").mkdir();
        }
        if (absentDesktop(x32Path)) {
            new File(x32Path, "Desktop").mkdir();
        }
    }

    private boolean absentDesktop(String directoryPath) {
        return exists(directoryPath) && !exists(directoryPath + "\\Desktop");
    }

    private boolean exists(String path) {
        return new File(path).exists();
    }

    private Thread createLockDetectorThread(final Process excelLaunchProcess) {
        return new Thread(() -> {
            try {
                Thread.sleep(LOCK_DETECT_TIMEOUT);
                try {
                    excelLaunchProcess.exitValue();
                } catch (IllegalThreadStateException e) {
                    log.error("ExcelLauncher is locked. Allow GUI interaction for service.");
                    excelLaunchProcess.destroy();
                }
            } catch (InterruptedException e) {
                // do nothing
            }
        });
    }

}
