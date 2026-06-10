import java.util.zip.ZipFile

try {
    // Maven Invoker writes the reactor build log at the IT project root. Assert it exists so the regression
    // check below can never be silently skipped.
    def buildLog = new File(basedir, 'build.log')
    assert buildLog.exists() : 'build.log was not found at the IT project root'
    assert !buildLog.text.contains('There is no implementation in rules for interface method') :
            'Duplicate datatype Class on verify: the generated classes jar must be excluded from the archive'

    // 'includeGeneratedClasspathJar' is not configured, so the exclusion must come from the automatic
    // decision (OpenL dependency + generate goal); pin the decision path via its INFO message.
    assert buildLog.text.contains('Excluding the generated classes jar from the archive') :
            'The automatic includeGeneratedClasspathJar decision did not report the exclusion'

    // The API project's main 'openl' archive (the .zip produced by the package goal); it is read below to
    // assert what it does and does not contain.
    def apiZip = new File(basedir, 'openl-clash-api/target/openl-clash-api-0.0.0.zip')
    assert apiZip.exists() : 'API openl archive openl-clash-api-0.0.0.zip was not produced'

    // Unset includeGeneratedClasspathJar + OpenL dependency + generate goal => the project's own generated
    // classes jar must NOT be packed into the archive, so the regenerated datatype is kept off the deployed
    // runtime classpath.
    new ZipFile(apiZip).withCloseable { zf ->
        def names = zf.entries().collect { it.name }
        assert !names.contains('lib/openl-clash-api-0.0.0.jar') :
                'The generated classes jar must be excluded from the archive by the automatic decision'
    }

    // The exclusion concerns only the in-archive copy: the supplementary 'classes' artifact must still be
    // built and attached, because that is what Java consumers (openl-clash-client) resolve from the reactor.
    assert new File(basedir, 'openl-clash-api/target/openl-clash-api-0.0.0-classes.jar').isFile() :
            'The classes artifact must be attached even when the jar is excluded from the archive'

    // The plain-Java consumer that implements the generated interface must have compiled (no ClassNotFound).
    assert new File(basedir, 'openl-clash-client/target/classes/com/example/client/Client.class').exists() :
            'The Java consumer implementing the generated interface failed to compile'

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
