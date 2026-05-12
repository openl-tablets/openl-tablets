try {
    File folder = basedir

    // The fixture exercises every migrator on a rules.xml that:
    //   * declares the project name equal to the folder name (drop-name rule),
    //   * lists a wildcard module with a redundant <name>,
    //   * declares the implicit groovy/ + lib/*.jar classpath entries (and the pom has no runtime
    //     dependencies, so packaging would not populate lib/),
    //   * uses the deprecated CWPropertyFileNameProcessor, and
    //   * has a <method-filter> on the module.
    // After migration every default-restating element disappears, leaving only the synthesised
    // <exposed-methods> derived from the previous <method-filter>.
    def expectedRulesXml = '''<project>
    <exposed-methods>
        <include>pong</include>
    </exposed-methods>
</project>
'''

    def expectedDeployXml = '''<rules-deploy>
    <serviceName>migrate-service</serviceName>
    <publishers>
        <publisher>RESTFUL</publisher>
    </publishers>
    <annotationTemplateClassName>LegacyService</annotationTemplateClassName>
    <url>foo</url>
    <version>v1</version>
</rules-deploy>
'''

    def expectedGroovy = '''import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
interface LegacyService {
    @GET
    @Path("/echo/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @XmlElement
    String echo(@PathParam("name") String name)
}
'''

    def actualRulesXml = new File(folder, 'rules.xml').getText('UTF-8').replace("\r\n", "\n")
    def actualDeployXml = new File(folder, 'rules-deploy.xml').getText('UTF-8').replace("\r\n", "\n")
    def actualGroovy = new File(folder, 'groovy/LegacyService.groovy').getText('UTF-8').replace("\r\n", "\n")

    assert actualRulesXml == expectedRulesXml : "rules.xml mismatch:\n--- expected ---\n${expectedRulesXml}\n--- actual ---\n${actualRulesXml}"
    assert actualDeployXml == expectedDeployXml : "rules-deploy.xml mismatch:\n--- expected ---\n${expectedDeployXml}\n--- actual ---\n${actualDeployXml}"
    assert actualGroovy == expectedGroovy : "LegacyService.groovy mismatch:\n--- expected ---\n${expectedGroovy}\n--- actual ---\n${actualGroovy}"

    def buildLog = new File(folder, 'build.log').getText('UTF-8')
    // Build log emits "Migrate: <file> (<commitMessage>)" for each descriptor phase (commit message comes
    // straight from Migrator.getCommitMessage()) and "Migrate <relativePath>" for each groovy file that
    // changed.
    assert buildLog.contains('Migrate: rules.xml (needless tags cleanup)')
    assert buildLog.contains('Migrate: rules.xml (drop lib/*.jar classpath entry — packaging does not populate lib/)')
    assert buildLog.contains('Migrate: rules.xml (drop default classpath from rules.xml)')
    assert buildLog.contains('Migrate: rules.xml (drop CWPropertyFileNameProcessor)')
    assert buildLog.contains('Migrate: rules.xml (method-filter to exposed-methods)')
    assert buildLog.contains('Migrate: rules.xml (drop redundant module and project-name defaults from rules.xml)')
    assert buildLog.contains('Migrate: rules-deploy.xml (needless tags cleanup)')
    assert buildLog.contains('Migrate: rules-deploy.xml (drop default isProvideRuntimeContext=false)')
    assert buildLog.contains('Migrate: rules-deploy.xml (interceptingTemplateClassName to annotationTemplateClassName)')
    assert buildLog.contains('Migrate groovy/LegacyService.groovy')

    // Tally every "Migrate: " line so an unexpected migrator (e.g. someone wires a noisy migrator without
    // updating this script) immediately surfaces. rules.xml: empty-tag + lib + classpath + cw-processor +
    // method-filter + default-modules = 6 lines; rules-deploy.xml: empty-tag + runtime-context +
    // template-class = 3 lines; total = 9. (GroovyJakartaMigrator emits "Migrate <path>", not
    // "Migrate: ", so it doesn't count here.)
    def migrateColonLines = buildLog.readLines().findAll { it.contains('Migrate: ') }
    assert migrateColonLines.size() == 9 :
            "expected exactly 9 'Migrate: ' lines, got ${migrateColonLines.size()}:\n${migrateColonLines.join('\n')}"

    // setup.groovy created a fresh local git repository and made an "initial" commit. After openl:migrate
    // every migrator that actually changed files produced its own SCM commit. Read the log subjects (one per
    // line) and assert the commit messages follow the configured template.
    def gitLog = ['git', 'log', '--format=%s'].execute(null, folder)
    gitLog.waitFor()
    def subjects = gitLog.text.split('\n').findAll { it }
    assert subjects.contains('initial: imported IT fixture') : "missing setup commit; got:\n${subjects.join('\n')}"

    // Every migrate commit subject must start with the configured comment prefix "migrate:" (followed by
    // a space from the template) and end with " for OpenL <version>". The bodies (Co-authored-by line)
    // live in the rest of the message, which we sample below.
    def migrateSubjects = subjects.findAll { it.startsWith('migrate: ') }
    assert !migrateSubjects.isEmpty() : "no migrate-prefixed commit subjects in:\n${subjects.join('\n')}"
    migrateSubjects.each { s ->
        assert s =~ /^migrate: {1,2}.+ for OpenL .+$/ : "unexpected commit subject: '${s}'"
    }

    // Migrators run in a fixed order; not all necessarily produce a commit (e.g. a no-op transform yields
    // no file change). We assert that the commits we DO expect for this fixture are present, identified
    // by the per-migrator commit subject.
    def commitFragments = subjects.collect { it.split(' for OpenL ')[0] } as Set
    assert commitFragments.contains('migrate: needless tags cleanup')
    assert commitFragments.contains('migrate: drop lib/*.jar classpath entry — packaging does not populate lib/')
    assert commitFragments.contains('migrate: drop default classpath from rules.xml')
    assert commitFragments.contains('migrate: method-filter to exposed-methods')
    assert commitFragments.contains('migrate: drop redundant module and project-name defaults from rules.xml')
    assert commitFragments.contains('migrate: drop CWPropertyFileNameProcessor')
    assert commitFragments.contains('migrate: drop default isProvideRuntimeContext=false')
    assert commitFragments.contains('migrate: interceptingTemplateClassName to annotationTemplateClassName')
    assert commitFragments.contains('migrate: groovy scripts from javax to jakarta')

    // The commit body must include the Co-authored-by trailer for every migration commit. Sample one.
    def lastCommit = ['git', 'log', '-1', '--format=%B'].execute(null, folder)
    lastCommit.waitFor()
    def lastCommitMessage = lastCommit.text
    assert lastCommitMessage.contains('Co-authored-by: openl-maven-plugin:') : "missing Co-authored-by trailer in last commit:\n${lastCommitMessage}"
    assert lastCommitMessage.contains('<openltablets@eisgroup.com>') : "missing trailer mailbox in last commit:\n${lastCommitMessage}"

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
