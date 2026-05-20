try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text
    // `localRepositoryPath` is auto-injected by maven-invoker-plugin into post-build scripts;
    // points at whichever local repo Maven used to run the IT, cross-platform.
    def localRepo = localRepositoryPath

    // Pom-less project is auto-discovered via the SUBMODULE 'rules-root' that declares
    // openl-maven-plugin with <extensions>true</extensions>. The top-level pom does NOT declare
    // the plugin — this asserts the participant scans every reactor pom that does, not just the
    // top-level.
    //
    // rules-root/pom.xml ALSO declares its own <groupId>com.example.flatten</groupId>, overriding
    // the inherited 'com.example' from the top-level. With <flattenGroupId>true</> on that
    // submodule, the discovered pom-less project must land at com.example.flatten:generated —
    // proving coordinates derive from the ANCHOR's groupId, not the top-level's.
    //
    // The classic project (in <modules>) keeps the top-level groupId 'com.example' because its
    // own pom declares no override and is unaffected by the anchor's choice.
    assert logs.contains("Discovered pom-less OpenL project 'com.example.flatten:generated'"),
            "anchor's own groupId must be used; expected com.example.flatten:generated"
    assert !logs.contains("Discovered pom-less OpenL project 'com.example:generated'"),
            "must NOT fall back to the top-level groupId 'com.example' for projects under rules-root"
    assert !logs.contains("Discovered pom-less OpenL project 'com.example.flatten.deep.nested:generated'"),
            "with flattenGroupId=true the path segments must NOT be appended to the groupId"
    assert !logs.contains("Discovered pom-less OpenL project 'com.example:classic'"),
            "Classic project must not be auto-discovered (it has its own pom.xml)"
    assert !logs.contains("Discovered pom-less OpenL project 'com.example.flatten:rules-root'"),
            "The anchor submodule itself must not be discovered as a pom-less project"

    // Pom-less root stays clean; generated XML pom lives in target/ only.
    def generatedRoot = new File(folder, 'rules-root/deep/nested/generated')
    assert !new File(generatedRoot, '.openl-pom.xml').exists()
    assert new File(generatedRoot, 'target/openl-pom.xml').exists()

    // Classic project produces a zip via its own pom; pom-less project via the participant.
    assert new File(folder, 'classic/target/classic-0.0.0.zip').exists()
    assert new File(generatedRoot, 'target/generated-0.0.0.zip').exists()

    // Classic stays at com.example (inherits from top-level, no override).
    assert new File(localRepo, 'com/example/classic/0.0.0/classic-0.0.0.zip').exists()
    assert new File(localRepo, 'com/example/classic/0.0.0/classic-0.0.0.pom').exists()

    // Pom-less 'generated' lands at the anchor's OWN groupId (com.example.flatten), NOT the
    // top-level's (com.example) — and NOT a path-derived com.example.flatten.deep.nested.
    def generatedPom = new File(localRepo, 'com/example/flatten/generated/0.0.0/generated-0.0.0.pom')
    assert generatedPom.exists(),
            "generated pom must install under the anchor's own groupId path com/example/flatten/"
    assert new File(localRepo, 'com/example/flatten/generated/0.0.0/generated-0.0.0.zip').exists()
    assert !new File(localRepo, 'com/example/generated/0.0.0/generated-0.0.0.pom').exists(),
            "must NOT install under the top-level groupId 'com.example'"
    assert !new File(localRepo, 'com/example/flatten/deep/nested/generated/0.0.0/generated-0.0.0.pom').exists(),
            "no installation under path-derived coordinates is expected with flattenGroupId=true"

    def pomText = generatedPom.text
    assert !pomText.contains('<parent>'), "generated (pom-less) installed pom must not declare <parent>"
    assert !pomText.contains('<build>'),  "generated (pom-less) installed pom must not declare <build>"
    assert pomText.contains('<groupId>com.example.flatten</groupId>'),
            "installed pom must carry the anchor's own groupId"
    assert pomText.contains('<artifactId>generated</artifactId>')

    // Anchor's <build><plugins> must inherit into pom-less projects' effective model. The anchor
    // declares maven-antrun-plugin bound to generate-sources; it must fire BOTH on the anchor
    // itself AND on the pom-less 'generated' project, and ${project.artifactId} must interpolate
    // against the child's effective model — not the anchor's.
    assert logs.contains("ANCHOR-PLUGIN ran on artifact=rules-root group=com.example.flatten"),
            "anchor's antrun execution must fire on the anchor itself"
    assert logs.contains("ANCHOR-PLUGIN ran on artifact=generated group=com.example.flatten"),
            "anchor's antrun execution must inherit into the pom-less 'generated' project; the property reference must resolve against the child's model, not the anchor's"

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
