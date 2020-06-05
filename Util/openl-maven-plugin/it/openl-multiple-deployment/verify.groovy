import java.util.zip.ZipFile

try {
    File folder = basedir

    def childProjectZips = new File(folder, 'openl-child-dependency/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })

    assert childProjectZips.length == 1

    def childZip = new ZipFile(childProjectZips[0])
    assert childZip.entries().findAll{ !it.directory && it.name == "rules.xml" }.size() == 1
    assert childZip.entries().findAll{ !it.directory && it.name == "Project2-Main.xlsx" }.size() == 1
    assert childZip.entries().findAll{ !it.directory && it.name.contains("-Test.xlsx")}.size() == 0

    def parentProjectZips = new File(folder, 'openl-parent-project/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })

    assert parentProjectZips.length == 2
    def rulesArchive = parentProjectZips.find{ it.name == "openl-parent-project-0.0.0-deployment.zip"}
    assert rulesArchive != null

    def deploymentZip = new ZipFile(rulesArchive)
    assert deploymentZip.entries().findAll{ !it.directory && it.name == "deployment.yaml" }.size() == 1
    assert deploymentZip.entries().findAll{ !it.directory && it.name == "openl-parent-project/rules.xml" }.size() == 1
    assert deploymentZip.entries().findAll{ !it.directory && it.name == "openl-parent-project/Project1-Main.xlsx" }.size() == 1
    assert deploymentZip.entries().findAll{ !it.directory && it.name == "openl-child-dependency/rules.xml" }.size() == 1
    assert deploymentZip.entries().findAll{ !it.directory && it.name == "openl-child-dependency/Project2-Main.xlsx" }.size() == 1
    assert deploymentZip.entries().findAll{ !it.directory && it.name.contains("-Test.xlsx")}.size() == 0

    rulesArchive = parentProjectZips.find{ it.name == "openl-parent-project-0.0.0.zip"}
    assert rulesArchive != null

    def parentZip = new ZipFile(rulesArchive)
    assert parentZip.entries().findAll{ !it.directory && it.name == "rules.xml" }.size() == 1
    assert parentZip.entries().findAll{ !it.directory && it.name == "Project1-Main.xlsx" }.size() == 1
    assert parentZip.entries().findAll{ !it.directory && it.name.contains("-Test.xlsx")}.size() == 0

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('OpenL Plugin: Multiple deployment .................. SUCCESS') }
    assert lines.any { it.contains('OpenL Plugin: Child Project ........................ SUCCESS') }
    assert lines.any { it.contains('Running sayHelloTest from the module Child-Test') }
    assert lines.any { it.contains('OpenL Plugin: Parent Project ....................... SUCCESS') }
    assert lines.any { it.contains('Running spr from the module Parent-Test') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}