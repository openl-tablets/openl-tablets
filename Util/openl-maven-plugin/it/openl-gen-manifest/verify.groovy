import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.jar.Attributes
import java.util.jar.JarInputStream

Attributes readManifest(String path) {
    def zipFile = new File(basedir, path).listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })
    assert zipFile.length == 1
    return new JarInputStream(zipFile[0].newInputStream()).manifest?.mainAttributes
}

try {

    def defManifest = readManifest('openl-default-manifest/target')
    assert defManifest.size() == 7
    assert defManifest.getValue('Manifest-Version') == '1.0'
    assert defManifest.getValue('Implementation-Title') == 'org.openl.internal:openl-default-manifest'
    assert defManifest.getValue('Implementation-Version') == '0.0.0'
    assert defManifest.getValue('Implementation-Vendor') == 'OpenL Tablets'
    assert defManifest.getValue('Created-By') ==~ /OpenL Maven Plugin v.+/
    assert defManifest.getValue('Built-By') ==~ /\S+/
    assert ZonedDateTime.parse(defManifest.getValue('Build-Date')) <= ZonedDateTime.now()

    def customManifest = readManifest('openl-custom-manifest/target')
    assert customManifest.size() == 10
    assert customManifest.getValue('Manifest-Version') == '1.0'
    assert customManifest.getValue('Implementation-Title') == 'My Title'
    assert customManifest.getValue('Implementation-Version') == 'My Version'
    assert customManifest.getValue('Implementation-Vendor') == 'My Vendor'
    assert customManifest.getValue('Build-Number') == '1e1eb11271dd'
    assert customManifest.getValue('Build-Branch') == 'myBranch'
    assert customManifest.getValue('Created-By') ==~ /OpenL Maven Plugin v.+/
    assert customManifest.getValue('Built-By') == 'superuser'
    assert customManifest.getValue('Name') == 'OpenL Plugin: Custom Manifest'
    assert LocalDateTime.parse(customManifest.getValue('Build-Date'), DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm')) != null

    def disabledManifest = readManifest('openl-disabled-manifest/target')
    assert disabledManifest.size() == 3
    assert disabledManifest.getValue('Manifest-Version') == '1.0'
    assert disabledManifest.getValue('Build-Number') == '1e1eb11271dd'
    assert disabledManifest.getValue('Build-Branch') == 'myBranch'

    assert readManifest('openl-no-manifest/target') == null

} catch(Throwable e) {
    e.printStackTrace()
    return false
}