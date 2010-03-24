/*
 * Created on Dec 15, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.webtools;



/**
 * 
 * @author sam
 */
public class ExcelLauncher
{
  String scriptName;
  

  String wbPath;
  String wbName;
  String wsName;
  String range;

  public void launch() throws Exception
  {
	if (range == null || range.equals("null"))
		range="A1";
    String[] cmdarray =
      { "wscript", scriptName, wbPath, wbName, wsName == null ? "1" : wsName, range, };

    Runtime.getRuntime().exec(cmdarray).waitFor();
  }

  public ExcelLauncher(
    String scriptName,
    String wbPath,
    String wbName,
    String wsName,
    String range)
  {
    this.scriptName = scriptName;
    this.wbPath = wbPath;
    this.wbName = wbName;
    this.wsName = wsName;
    this.range = range;
  }

  static public void launch(String url) throws Exception
  {
    XlsUrlParser p = new XlsUrlParser();
    p.parse(url);

    launch(p.scriptName, p.wbPath, p.wbName, p.wsName, p.range);
  }

  static public void launch(
    String scriptName,
    String wbPath,
    String wbName,
    String wsName,
    String range)
    throws Exception
  {
    ExcelLauncher l =
      new ExcelLauncher(Launcher.getLaunchScriptsDir() + "/" + scriptName, wbPath, wbName, wsName, range);

    l.launch();
  }

}
