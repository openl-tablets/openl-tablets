try {
    File folder = basedir

    // The 'service' project publishes normally and therefore gets a deployment supplementary artifact.
    def serviceMain = new File(folder, 'service/target/service-0.0.0.zip')
    def serviceDeployment = new File(folder, 'service/target/service-0.0.0-deployment.zip')
    assert serviceMain.exists() : "Expected main artifact '${serviceMain}' to be present."
    assert serviceDeployment.exists() : "Expected deployment artifact '${serviceDeployment}' to be present."

    // The 'silent-consumer' project declares empty <publishers/> in rules-deploy.xml. Even though it has
    // an OpenL dependency on 'no-xlsx-common' (which would normally trigger deployment auto-detection),
    // the packaging plugin must skip the deployment supplementary artifact.
    def silentMain = new File(folder, 'silent-consumer/target/silent-consumer-0.0.0.zip')
    def silentDeployment = new File(folder, 'silent-consumer/target/silent-consumer-0.0.0-deployment.zip')
    assert silentMain.exists() : "Expected main artifact '${silentMain}' to be present."
    assert !silentDeployment.exists() : "Deployment artifact must NOT be produced for a project with empty <publishers/>, but found '${silentDeployment}'."

    // PackageMojo must explicitly log why the deployment artifact was skipped (this is the user-facing
    // signal). VerifyMojo skips silently via isDisabled() — same pattern as -DskipTests — so we don't
    // assert a log line for it. Instead, assert no failure was reported AND that the OPENL VERIFY
    // header didn't fire for silent-consumer (the disable check happens before the header banner).
    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains("declares empty <publishers/>; skipping the deployment artifact") } :
            "Expected build.log to contain the empty-publishers skip message."
    int silentVerifyStart = lines.findIndexOf {
        it.contains("openl:") && it.contains(":verify (default-verify) @ silent-consumer")
    }
    assert silentVerifyStart != -1 : "Expected the verify goal to fire for silent-consumer."
    // The next non-blank lines after the verify goal banner should be either the next goal or another
    // module — NOT an "OPENL VERIFY" header (which would mean execute() ran).
    def afterVerify = lines.drop(silentVerifyStart + 1).take(5).join('\n')
    assert !afterVerify.contains('OPENL VERIFY') :
            "VerifyMojo must skip silent-consumer via isDisabled() (no OPENL VERIFY banner) when <publishers/> is empty."

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
