import java.util.zip.ZipFile
import java.util.zip.ZipEntry

static String findManifest(ZipFile zipFile) {
    def defManifest = zipFile.entries().find{ !it.directory && it.name == "META-INF/MANIFEST.MF" }
    assert defManifest != null
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    baos << zipFile.getInputStream(defManifest as ZipEntry)
    return baos.toString()
}

try {

    def defManifestZips = new File(basedir, 'openl-default-manifest/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })
    assert defManifestZips.length == 1
    def defManifestContent = findManifest(new ZipFile(defManifestZips[0])).split("\\r?\\n")
    assert defManifestContent.any { it == 'Manifest-Version: 1.0' }
    assert defManifestContent.any { it == 'Implementation-Title: org.openl.internal:openl-default-manifest' }
    assert defManifestContent.any { it == 'Implementation-Version: 0.0.0' }
    assert defManifestContent.any { it == 'Implementation-Vendor: OpenL Tablets' }
    assert defManifestContent.any { it ==~ /Created-By: OpenL Maven Plugin .+/ }
    assert defManifestContent.any { it ==~ /Built-By: .*/ }
    assert defManifestContent.any { it ==~ /Build-Date: \d{4}(-\d{2}){2}T(\d{2}:?){3}.+/ }
    assert defManifestContent.length == 7

    def customManifestZips = new File(basedir, 'openl-custom-manifest/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })
    assert customManifestZips.length == 1
    def customManifestContent = findManifest(new ZipFile(customManifestZips[0])).split("\\r?\\n")
    assert customManifestContent.any { it == 'Manifest-Version: 1.0' }
    assert customManifestContent.any { it == 'Implementation-Title: My Title' }
    assert customManifestContent.any { it == 'Implementation-Version: My Version' }
    assert customManifestContent.any { it == 'Implementation-Vendor: My Vendor' }
    assert customManifestContent.any { it == 'Build-Number: 1e1eb11271dd' }
    assert customManifestContent.any { it == 'Build-Branch: myBranch' }
    assert customManifestContent.any { it ==~ /Created-By: OpenL Maven Plugin .+/ }
    assert customManifestContent.any { it ==~ /Built-By: superuser/ }
    assert customManifestContent.any { it ==~ /Build-Date: \d{4}(-\d{2}){2}T(\d{2}:?){3}.+/ }
    assert customManifestContent.length == 9

    def disabledManifestZips = new File(basedir, 'openl-disabled-manifest/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })
    assert disabledManifestZips.length == 1
    assert new ZipFile(disabledManifestZips[0]).entries().find{ !it.directory && it.name == "META-INF/MANIFEST.MF" } == null

} catch(Throwable e) {
    e.printStackTrace()
    return false
}