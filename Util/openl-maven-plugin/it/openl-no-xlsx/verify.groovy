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

    // The build log should explicitly note the skip.
    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains("declares empty <publishers/>; skipping the deployment artifact") } :
            "Expected build.log to contain the empty-publishers skip message."

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
