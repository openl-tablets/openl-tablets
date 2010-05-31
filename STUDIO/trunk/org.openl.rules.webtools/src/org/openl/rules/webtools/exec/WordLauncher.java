/*
 * Created on Dec 15, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.webtools.exec;

/**
 * MS Word launcher.
 *
 * @author sam
 */
public class WordLauncher {

    String scriptName;

    String wdPath;
    String wdName;
    String wdParStart;
    String wdParEnd;

    public WordLauncher(String scriptName, String wdPath, String wdName, String wdParStart, String wdParEnd) {
        this.scriptName = scriptName;
        this.wdPath = wdPath;
        this.wdName = wdName;
        this.wdParStart = wdParStart;
        this.wdParEnd = wdParEnd;
    }

    static public void launch(String scriptName, String wdPath, String wdName, String wdParStart, String wdParEnd)
            throws Exception {
        WordLauncher l = new WordLauncher(scriptName, wdPath, wdName, wdParStart, wdParEnd);

        l.launch();
    }

    public void launch() throws Exception {
        if (wdParStart == null || wdParStart.equals("null")) {
            wdParStart = "1";
        }
        if (wdParEnd == null || wdParEnd.equals("null")) {
            wdParEnd = wdParStart;
        }

        String[] cmdarray = { "wscript", Launcher.getLaunchScriptsDir() + "/" + scriptName, wdPath, wdName, wdParStart,
                wdParEnd };

        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

}
