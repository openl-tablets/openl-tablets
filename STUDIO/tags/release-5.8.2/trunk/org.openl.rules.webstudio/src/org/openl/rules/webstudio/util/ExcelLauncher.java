/*
 * Created on Dec 15, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.webstudio.util;

/**
 * MS Excel launcher.
 * 
 * @author sam
 */
public class ExcelLauncher {

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
        String[] cmdarray = { "wscript", scriptPath, workbookPath, workbookName, worksheetName == null ? "1" : worksheetName, range, };

        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

    public static void launch(String scriptPath, String wbPath, String wbName, String wsName, String range) throws Exception {
        new ExcelLauncher(scriptPath, wbPath, wbName, wsName, range).launch();
    }

}
