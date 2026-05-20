import java.util.zip.ZipFile

try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text
    // `localRepositoryPath` is auto-injected by maven-invoker-plugin into post-build scripts;
    // points at whichever local repo Maven used to run the IT, cross-platform.
    def localRepo = localRepositoryPath

    // ---------------------------------------------------------------------------------------
    // 1. Participant discovery — each pom-less project announced exactly once with the
    //    coordinates derived from its folder path under the anchor.
    // ---------------------------------------------------------------------------------------
    def expectedProjects = [
            'com.example:shared'                : 'shared',
            'com.example.domain:core'           : 'domain/core',
            'com.example.domain:ext'            : 'domain/ext',
            'com.example.api:public-api'        : 'api/public-api',
            'com.example.api:internal-api'      : 'api/internal-api',
            'com.example.markets.eu.de:pricing' : 'markets/eu/de/pricing',
    ]
    expectedProjects.each { coord, _path ->
        def discoveryCount = logs.readLines().findAll {
            it.contains("Discovered pom-less OpenL project '${coord}'")
        }.size()
        assert discoveryCount == 1,
                "Expected exactly one discovery log for '${coord}', got ${discoveryCount}"
    }

    // ---------------------------------------------------------------------------------------
    // 2. Reactor — all six projects + anchor build, in correct topological order.
    // ---------------------------------------------------------------------------------------
    assert logs.contains('Reactor Build Order:')
    assert logs.contains('BUILD SUCCESS')
    def buildOrder = [
            'shared'      : logs.indexOf('Building shared 0.0.0'),
            'core'        : logs.indexOf('Building core 0.0.0'),
            'ext'         : logs.indexOf('Building ext 0.0.0'),
            'public-api'  : logs.indexOf('Building public-api 0.0.0'),
            'internal-api': logs.indexOf('Building internal-api 0.0.0'),
            'pricing'     : logs.indexOf('Building pricing 0.0.0'),
    ]
    buildOrder.each { name, idx -> assert idx > -1, "Project '${name}' did not build" }
    assert buildOrder['core'] < buildOrder['ext'],         "core must build before ext (name dep)"
    assert buildOrder['core'] < buildOrder['public-api'],  "core must build before public-api (mavenArtifact dep)"
    assert buildOrder['ext']  < buildOrder['internal-api'],"ext must build before internal-api (name dep)"
    assert buildOrder['shared']     < buildOrder['pricing'], "shared must build before pricing"
    assert buildOrder['public-api'] < buildOrder['pricing'], "public-api must build before pricing"

    // ---------------------------------------------------------------------------------------
    // 3. Per-project artefacts: target/<artifact>-0.0.0.zip + target/openl-pom.xml,
    //    and the same artefacts installed under the IT-isolated shared local repo.
    //    NO legacy .openl-pom.xml in any project root.
    // ---------------------------------------------------------------------------------------
    def projects = [
            'shared'      : [folder: 'shared',                       groupPath: 'com/example'],
            'core'        : [folder: 'domain/core',                  groupPath: 'com/example/domain'],
            'ext'         : [folder: 'domain/ext',                   groupPath: 'com/example/domain'],
            'public-api'  : [folder: 'api/public-api',               groupPath: 'com/example/api'],
            'internal-api': [folder: 'api/internal-api',             groupPath: 'com/example/api'],
            'pricing'     : [folder: 'markets/eu/de/pricing',        groupPath: 'com/example/markets/eu/de'],
    ]
    projects.each { name, info ->
        def root = new File(folder, info.folder)
        assert !new File(root, '.openl-pom.xml').exists(),
                "Project root '${info.folder}' must not contain legacy .openl-pom.xml"

        def target = new File(root, 'target')
        assert new File(target, 'openl-pom.xml').exists(), "Missing target/openl-pom.xml for '${name}'"
        def zip = new File(target, "${name}-0.0.0.zip")
        assert zip.exists(), "Missing packaged artifact ${zip}"

        new ZipFile(zip).withCloseable { closeable ->
            ZipFile zf = closeable as ZipFile
            def names = zf.entries().collect { it.name }
            assert names.contains('rules.xml')
            assert names.any { it.endsWith("${name}.xlsx") }
            assert !names.contains('.openl-pom.xml')
            assert !names.contains('openl-pom.xml')
        }

        def repoDir = new File(localRepo, "${info.groupPath}/${name}/0.0.0")
        assert new File(repoDir, "${name}-0.0.0.zip").exists(), "Missing installed zip for '${name}'"
        assert new File(repoDir, "${name}-0.0.0.pom").exists(), "Missing installed pom for '${name}'"
    }

    // ---------------------------------------------------------------------------------------
    // 4. Installed pom shape — minimal Maven XML: GAV + packaging + dependencies. No <build>,
    //    no <parent>. Versions are literal, not ${project.version} placeholders.
    // ---------------------------------------------------------------------------------------
    projects.each { name, info ->
        def pom = new File(localRepo, "${info.groupPath}/${name}/0.0.0/${name}-0.0.0.pom").text
        assert !pom.contains('<parent>'),                 "${name} installed pom must not declare <parent>"
        assert !pom.contains('<build>'),                  "${name} installed pom must not declare <build>"
        assert !pom.contains('<repositories>'),           "${name} installed pom must not leak super-pom <repositories>"
        assert !pom.contains('<pluginRepositories>'),     "${name} installed pom must not leak super-pom <pluginRepositories>"
        assert !pom.contains('<reporting>'),              "${name} installed pom must not leak super-pom <reporting>"
        assert !pom.contains('${project.version}'),       "${name} installed pom must not contain unresolved \${project.version}"
        assert pom.contains("<artifactId>${name}</artifactId>")
        assert pom.contains('<version>0.0.0</version>')
        assert pom.contains('<packaging>openl</packaging>')
    }

    // core has no <dependencies>; shared carries a single jar mavenArtifact (commons-text).
    def corePom = new File(localRepo, "${projects['core'].groupPath}/core/0.0.0/core-0.0.0.pom").text
    assert !corePom.contains('<dependencies>'), "core must not declare any <dependencies>"

    // ---------------------------------------------------------------------------------------
    // 4a. Jar <mavenArtifact> in rules.xml — must land in the installed pom as
    //     <optional>true</> so downstream OpenL consumers don't inherit it transitively.
    //     <type>jar</> is Maven's implicit default and is omitted from serialised poms; we
    //     assert <optional>true</> AND that the dep is NOT typed as zip.
    // ---------------------------------------------------------------------------------------
    def sharedPom = new File(localRepo, "${projects['shared'].groupPath}/shared/0.0.0/shared-0.0.0.pom").text
    assert sharedPom =~ /(?s)<dependency>\s*<groupId>org\.apache\.commons<\/groupId>\s*<artifactId>commons-text<\/artifactId>\s*<version>1\.15\.0<\/version>\s*<optional>true<\/optional>\s*<\/dependency>/,
            "shared's installed pom must carry commons-text:1.15.0 with <optional>true</> (and default jar type — no <type> tag)"
    assert !(sharedPom =~ /<artifactId>commons-text<\/artifactId>[\s\S]*?<type>zip<\/type>/),
            "shared's commons-text must NOT be typed as zip — jar is the OpenL-side optional default for plain Java libs"

    // ext → core resolved by <name> (literal version).
    def extPom = new File(localRepo, 'com/example/domain/ext/0.0.0/ext-0.0.0.pom').text
    assert extPom =~ /(?s)<dependency>\s*<groupId>com\.example\.domain<\/groupId>\s*<artifactId>core<\/artifactId>\s*<version>0\.0\.0<\/version>\s*<type>zip<\/type>/

    // public-api → core via explicit <mavenArtifact>. The rules.xml carries a 99.99.99 placeholder
    // that the anchor's <dependencyManagement> rewrites to 0.0.0 — the installed pom must show the
    // managed value, not the placeholder.
    def publicApiPom = new File(localRepo, 'com/example/api/public-api/0.0.0/public-api-0.0.0.pom').text
    assert !publicApiPom.contains('99.99.99'),
            "anchor <dependencyManagement> should have overridden the rules.xml mavenArtifact placeholder"
    assert publicApiPom =~ /(?s)<dependency>\s*<groupId>com\.example\.domain<\/groupId>\s*<artifactId>core<\/artifactId>\s*<version>0\.0\.0<\/version>\s*<type>zip<\/type>/,
            "public-api installed pom must declare core 0.0.0 (managed by anchor)"

    // internal-api → ext via <name>.
    def internalApiPom = new File(localRepo, 'com/example/api/internal-api/0.0.0/internal-api-0.0.0.pom').text
    assert internalApiPom.contains('<artifactId>ext</artifactId>')

    // pricing → shared (name) AND public-api (mavenArtifact).
    def pricingPom = new File(localRepo, 'com/example/markets/eu/de/pricing/0.0.0/pricing-0.0.0.pom').text
    assert pricingPom.contains('<artifactId>shared</artifactId>')
    assert pricingPom.contains('<artifactId>public-api</artifactId>')
    assert pricingPom.contains('<groupId>com.example.api</groupId>')

    // ---------------------------------------------------------------------------------------
    // 4b. Transitive deps packaged into the OpenL zip's lib/ — shared declares commons-text
    //     directly; commons-lang3 comes along as commons-text's transitive. Both must be in
    //     shared's lib/ (PackageMojo's filter rule 4: transitive's trail index 1 = direct dep
    //     = commons-text, which is in shared's 'allowed' GA-set; rule 2: scope=compile passes).
    // ---------------------------------------------------------------------------------------
    def sharedZip = new File(folder, 'shared/target/shared-0.0.0.zip')
    def sharedLibEntries = []
    new ZipFile(sharedZip).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        sharedLibEntries = zf.entries()
                .collect { it.name }
                .findAll { it.startsWith('lib/') && it.endsWith('.jar') }
    }
    assert sharedLibEntries.any { it == 'lib/commons-text-1.15.0.jar' },
            "shared/target/shared-0.0.0.zip must package commons-text-1.15.0.jar into lib/ (got: ${sharedLibEntries})"
    assert sharedLibEntries.any { it ==~ /lib\/commons-lang3-[\d.]+\.jar/ },
            "shared/target/shared-0.0.0.zip must package commons-lang3 transitive into lib/ (got: ${sharedLibEntries})"

    // ---------------------------------------------------------------------------------------
    // 4c. Downstream OpenL consumers must NOT inherit the jar (or its transitives) — that's
    //     exactly why we emit <optional>true</> on jar deps. pricing depends on shared (zip);
    //     without the optional flag, commons-text + commons-lang3 would propagate via Maven's
    //     transitive resolution and land in pricing's lib/ too.
    // ---------------------------------------------------------------------------------------
    def pricingZip = new File(folder, 'markets/eu/de/pricing/target/pricing-0.0.0.zip')
    new ZipFile(pricingZip).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def pricingLibEntries = zf.entries()
                .collect { it.name }
                .findAll { it.startsWith('lib/') && it.endsWith('.jar') }
        assert !pricingLibEntries.any { it.contains('commons-text') },
                "pricing must NOT package commons-text — shared's optional=true should stop transitive propagation (got: ${pricingLibEntries})"
        assert !pricingLibEntries.any { it.contains('commons-lang3') },
                "pricing must NOT package commons-lang3 — second-order transitive must be blocked too (got: ${pricingLibEntries})"
    }

    // ---------------------------------------------------------------------------------------
    // 4d. Deployment BOM — attached to the anchor with classifier 'deployment-bom', type 'pom'.
    //     <dependencyManagement>: main entry for every OpenL project + an additional
    //         <classifier>deployment</> entry for projects predicted to produce *-deployment.zip
    //         (i.e. has any OpenL dep AND non-empty publishers in rules-deploy.xml). Independent
    //         of bundle eligibility.
    //     <dependencies>: ONLY projects that can be deployed — rules-deploy.xml does NOT declare
    //         empty <publishers/>. Deployment-classifier entry when the project produces one,
    //         else main entry.
    //     pomless-complex layout:
    //         - shared: leaf (no OpenL deps) + no rules-deploy.xml → deployable → main in bundle.
    //         - core:   leaf (no OpenL deps) + EMPTY <publishers/> in rules-deploy.xml → NOT
    //                   deployable → skipped from bundle but stays in dependencyManagement.
    //         - ext, public-api, internal-api, pricing: have OpenL deps + no rules-deploy.xml →
    //                   deployable → deployment-classifier in bundle.
    // ---------------------------------------------------------------------------------------
    def bomFile = new File(localRepo, 'com/example/openl-pomless-complex/0.0.0/openl-pomless-complex-0.0.0-deployment-bom.pom')
    assert bomFile.exists(),
            "deployment BOM must be installed under the anchor's GA with classifier 'deployment-bom'"
    def bomText = bomFile.text
    assert bomText.contains('<packaging>pom</packaging>'), "BOM must declare packaging=pom"
    assert bomText.contains('<dependencyManagement>'),    "BOM must carry <dependencyManagement>"
    assert bomText =~ /(?s)<\/dependencyManagement>\s*<dependencies>/,
            "BOM must also carry <dependencies> for one-shot bundle usage"

    // Split the BOM into its two sections so per-section assertions are unambiguous.
    def dmSection = (bomText =~ /(?s)<dependencyManagement>(.*?)<\/dependencyManagement>/).with { it.find() ? it.group(1) : '' }
    def depsSection = (bomText =~ /(?s)<\/dependencyManagement>\s*<dependencies>(.*?)<\/dependencies>/).with { it.find() ? it.group(1) : '' }
    assert dmSection,   "BOM <dependencyManagement> body must be non-empty"
    assert depsSection, "BOM <dependencies> body must be non-empty"

    def mainEntryRegex   = { String g, String a -> ~/(?s)<dependency>\s*<groupId>${java.util.regex.Pattern.quote(g)}<\/groupId>\s*<artifactId>${java.util.regex.Pattern.quote(a)}<\/artifactId>\s*<version>0\.0\.0<\/version>\s*<type>zip<\/type>\s*<\/dependency>/ }
    def deployEntryRegex = { String g, String a -> ~/(?s)<dependency>\s*<groupId>${java.util.regex.Pattern.quote(g)}<\/groupId>\s*<artifactId>${java.util.regex.Pattern.quote(a)}<\/artifactId>\s*<version>0\.0\.0<\/version>\s*<type>zip<\/type>\s*<classifier>deployment<\/classifier>\s*<\/dependency>/ }

    // Three categories — the BOM mojo treats each differently.
    String deployableLeaf    = 'com.example:shared'            // no OpenL deps, deployable → bundle holds main entry.
    String nonDeployableLeaf = 'com.example.domain:core'       // empty publishers → SKIPPED from bundle.
    List<String> deployableWithDeps = [                         // has OpenL deps, deployable → bundle holds deployment-classifier.
            'com.example.domain:ext',
            'com.example.api:public-api',
            'com.example.api:internal-api',
            'com.example.markets.eu.de:pricing',
    ]

    // Deployable leaf — main entry in BOTH sections; no deployment-classifier anywhere.
    def deployableLeafParts = deployableLeaf.tokenize(':')
    String deployableLeafG = deployableLeafParts[0]
    String deployableLeafA = deployableLeafParts[1]
    assert dmSection   =~ mainEntryRegex(deployableLeafG, deployableLeafA),    "BOM dependencyManagement must contain main entry for deployable leaf '${deployableLeaf}'"
    assert depsSection =~ mainEntryRegex(deployableLeafG, deployableLeafA),    "BOM <dependencies> must contain main entry for deployable leaf '${deployableLeaf}'"
    assert !(dmSection   =~ deployEntryRegex(deployableLeafG, deployableLeafA)), "BOM must NOT contain deployment-classifier for leaf '${deployableLeaf}'"
    assert !(depsSection =~ deployEntryRegex(deployableLeafG, deployableLeafA)), "BOM <dependencies> must NOT contain deployment-classifier for leaf '${deployableLeaf}'"

    // Non-deployable leaf — main entry in dependencyManagement ONLY; absent from bundle.
    def nonDeployableParts = nonDeployableLeaf.tokenize(':')
    String nonDeployG = nonDeployableParts[0]
    String nonDeployA = nonDeployableParts[1]
    assert dmSection   =~ mainEntryRegex(nonDeployG, nonDeployA),    "BOM dependencyManagement must still manage version for '${nonDeployableLeaf}' even though it's not deployable"
    assert !(depsSection =~ mainEntryRegex(nonDeployG, nonDeployA)), "BOM <dependencies> must SKIP '${nonDeployableLeaf}' because rules-deploy.xml declares empty <publishers/>"
    assert !(dmSection   =~ deployEntryRegex(nonDeployG, nonDeployA)), "BOM must NOT contain deployment-classifier for '${nonDeployableLeaf}' (it produces no deployment artefact)"
    assert !(depsSection =~ deployEntryRegex(nonDeployG, nonDeployA)), "BOM <dependencies> must NOT contain deployment-classifier for '${nonDeployableLeaf}'"

    deployableWithDeps.each { coord ->
        def (g, a) = coord.tokenize(':')
        assert dmSection   =~ mainEntryRegex(g, a),    "BOM dependencyManagement must include main entry for '${coord}'"
        assert dmSection   =~ deployEntryRegex(g, a),  "BOM dependencyManagement must include deployment-classifier entry for '${coord}'"
        assert depsSection =~ deployEntryRegex(g, a),  "BOM <dependencies> bundle must list the deployment-classifier entry for '${coord}'"
        // The bundle must NOT also list the main entry — consumers should get a single artefact per project.
        assert !(depsSection =~ mainEntryRegex(g, a)), "BOM <dependencies> must NOT also list the main entry for '${coord}' (the deployment artefact contains it)"
    }

    // Sanity: PackageMojo actually produced *-deployment.zip for the projects we predicted.
    deployableWithDeps.each { coord ->
        def (_g, a) = coord.tokenize(':')
        def projectFolder = projects[a].folder
        def deploymentZip = new File(folder, "${projectFolder}/target/${a}-0.0.0-deployment.zip")
        assert deploymentZip.exists(),
                "PackageMojo must produce ${a}-0.0.0-deployment.zip (BOM predicted it would); got missing ${deploymentZip}"
    }
    [deployableLeaf, nonDeployableLeaf].each { coord ->
        def (_g, a) = coord.tokenize(':')
        def projectFolder = projects[a].folder
        def deploymentZip = new File(folder, "${projectFolder}/target/${a}-0.0.0-deployment.zip")
        assert !deploymentZip.exists(),
                "Leaf '${a}' must NOT produce a deployment artefact (no OpenL deps); got unexpected ${deploymentZip}"
    }

    // BOM goal must have actually run on the anchor — log evidence.
    assert logs =~ /openl:[^:]+:prepare-bom \(default-prepare-bom\) @ openl-pomless-complex/,
            "openl:prepare-bom must run on the anchor"
    assert logs.contains("Attached deployment BOM (6 OpenL projects)"),
            "log must announce the deployment BOM with the discovered project count"

    // ---------------------------------------------------------------------------------------
    // 5. openl:compile and openl:package executed per pom-less project.
    // ---------------------------------------------------------------------------------------
    projects.each { name, _info ->
        assert logs =~ /openl:[^:]+:compile \(default-compile\) @ ${java.util.regex.Pattern.quote(name)}/,
                "openl:compile did not run for '${name}'"
        assert logs =~ /openl:[^:]+:package \(default-package\) @ ${java.util.regex.Pattern.quote(name)}/,
                "openl:package did not run for '${name}'"
        assert logs =~ /openl:[^:]+:prepare-pom \(default-prepare-pom\) @ ${java.util.regex.Pattern.quote(name)}/,
                "openl:prepare-pom did not run for '${name}'"
    }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
