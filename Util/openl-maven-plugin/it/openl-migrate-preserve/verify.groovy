try {
    File folder = basedir

    // The fixture is shaped so no rules.xml migrator fires: the project name differs from the folder
    // (drop-name rule stays silent), there is no <classpath> (classpath/lib rules stay silent), the
    // module name is not the rules-root basename and the path has no wildcard (default-modules rule
    // stays silent), and the configured processor is the default (cw-processor rule stays silent).
    // rules.xml must come out byte-identical to the input.
    def expectedRulesXml = '''<project>
    <name>preserve-fixture-name</name>
    <modules>
        <module>
            <name>main</name>
            <rules-root path="rules/Hello.xlsx"/>
        </module>
    </modules>
    <properties-file-name-pattern>%lob%-%state%</properties-file-name-pattern>
    <properties-file-name-processor>org.openl.rules.project.resolving.DefaultPropertiesFileNameProcessor</properties-file-name-processor>
    <exposed-methods>
        <include>pong</include>
    </exposed-methods>
</project>
'''

    def expectedDeployXml = '''<rules-deploy>
    <isProvideRuntimeContext>true</isProvideRuntimeContext>
    <serviceName>preserve-service</serviceName>
    <publishers>
        <publisher>RESTFUL</publisher>
    </publishers>
    <annotationTemplateClassName>LegacyService</annotationTemplateClassName>
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
    assert actualGroovy == expectedGroovy : "LegacyService.groovy must remain byte-identical when already in jakarta form:\n--- expected ---\n${expectedGroovy}\n--- actual ---\n${actualGroovy}"

    def buildLog = new File(folder, 'build.log').getText('UTF-8')
    // "Migrate: <file> (<commitMessage>)" is emitted only when the file is actually rewritten. On this
    // already-canonical fixture only the template-class migrator changes rules-deploy.xml (it drops
    // intercepting=UnknownService because the annotation slot already has LegacyService); every other
    // migrator is a true no-op.
    assert buildLog.contains('Migrate: rules-deploy.xml (interceptingTemplateClassName to annotationTemplateClassName)')

    // Tally every "Migrate: " line so an unexpected migrator immediately surfaces. Only the single
    // asserted line above should appear — every other migrator is a true no-op on this fixture.
    def migrateColonLines = buildLog.readLines().findAll { it.contains('Migrate: ') }
    assert migrateColonLines.size() == 1 :
            "expected exactly 1 'Migrate: ' line, got ${migrateColonLines.size()}:\n${migrateColonLines.join('\n')}"
    // GroovyJakartaMigrator only logs files that actually changed — LegacyService.groovy already uses jakarta.*,
    // so there must be no migration log entry for it.
    assert !(buildLog =~ /(?m)\[INFO\] Migrate .*\.groovy/) : "GroovyJakartaMigrator must not touch an already-migrated script"
    // openl:verify must succeed against the migrated runtime-context-aware service.
    assert buildLog.contains("Service 'openl-migrate-preserve-0.0.0' has been deployed successfully.") :
            "openl:verify did not deploy the runtime-context-aware service"

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
