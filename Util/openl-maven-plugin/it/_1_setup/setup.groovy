StringBuilder modules = new StringBuilder(10000);

File folder = basedir
def itFolder = folder.getParentFile()
for (def test : itFolder.listFiles()) {
    if (!test.isDirectory() || test.getName().startsWith("_")) {
        // Skip processing not folders (files, links and etc.)
        continue
    }
    modules.append('        <module>../').append(test.getName()).append('</module>\n')
}

println(modules)

def pomFile = new File(itFolder, '_2_install/pom.xml')
def pomText = pomFile.getText('UTF-8').replace('        <module>generated.pom</module>', modules);
pomFile.write(pomText, 'UTF-8')

return true
