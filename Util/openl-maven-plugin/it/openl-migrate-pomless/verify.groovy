try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    // ---------------------------------------------------------------------------------------
    // openl:pomless (dryRun=false) deletes the pom.xml of cleanly-convertible classic
    // OpenL projects and leaves blocked ones untouched.
    // ---------------------------------------------------------------------------------------

    // clean-leaf: bare openl plugin, no deps → convertible → pom.xml deleted, rules.xml kept.
    assert !new File(folder, 'clean-leaf/pom.xml').exists(),
            "clean-leaf/pom.xml must be deleted (cleanly convertible)"
    assert new File(folder, 'clean-leaf/rules.xml').exists(),
            "clean-leaf/rules.xml must remain — only the pom is removed"

    // with-dep: convertible → pom.xml deleted. Its optional compile jar (commons-text) moves into
    // with-dep/rules.xml as a <mavenArtifact>; its provided dep (commons-lang3) goes to the anchor;
    // its OpenL sibling (clean-leaf) is merged into the EXISTING <dependency><name>Clean Leaf Domain</> entry.
    assert !new File(folder, 'with-dep/pom.xml').exists(),
            "with-dep/pom.xml must be deleted"
    def withDepRules = new File(folder, 'with-dep/rules.xml').text
    assert withDepRules.contains('<mavenArtifact>org.apache.commons:commons-text:jar:1.15.0</mavenArtifact>'),
            "with-dep's optional compile jar must be written into its rules.xml as a jar mavenArtifact (Aether g:a:ext:v order)"
    assert !withDepRules.contains('commons-lang3'),
            "the provided dep must NOT go to rules.xml (it's not packaged) — it belongs on the anchor"
    // jar mavenArtifact deps carry no <name>/<autoIncluded> — those are OpenL (zip) concepts.
    def jarDepBlock = (withDepRules =~ /(?s)<dependency>\s*<mavenArtifact>org\.apache\.commons:commons-text[^<]*<\/mavenArtifact>\s*<\/dependency>/)
    assert jarDepBlock.find(),
            "the jar dependency must be a bare <mavenArtifact> entry with no <name> or <autoIncluded>"

    // Sibling merge: clean-leaf's logical <name> (from its rules.xml) is "Clean Leaf Domain" — the
    // migrator must match the consumer's existing <name>Clean Leaf Domain</> entry and INJECT the
    // <mavenArtifact>, not append a new entry keyed on the artifactId "clean-leaf".
    def siblingMergeBlock = (withDepRules =~ /(?s)<dependency>\s*<name>Clean Leaf Domain<\/name>\s*<autoIncluded>true<\/autoIncluded>\s*<mavenArtifact>com\.example:clean-leaf:0\.0\.0<\/mavenArtifact>\s*<\/dependency>/)
    assert siblingMergeBlock.find(),
            "the OpenL sibling's <mavenArtifact> must land inside the matching <name>Clean Leaf Domain</> entry"
    assert !(withDepRules =~ /<name>\s*clean-leaf\s*<\/name>/),
            "a fresh <name>clean-leaf</> entry must NOT be created — the existing logical name wins"

    // with-threshold: only <dependenciesThreshold> in config → convertible → pom.xml deleted.
    assert !new File(folder, 'with-threshold/pom.xml').exists(),
            "with-threshold/pom.xml must be deleted (a dependenciesThreshold-only config does not block)"
    assert new File(folder, 'with-threshold/rules.xml').exists()

    // blocked-config: skipITs config → blocked → pom.xml KEPT (its threshold still counts for the anchor max).
    assert new File(folder, 'blocked-config/pom.xml').exists(),
            "blocked-config/pom.xml must be kept (skipITs blocks conversion)"

    // ---------------------------------------------------------------------------------------
    // Report assertions — the plan is printed regardless of dry-run.
    // ---------------------------------------------------------------------------------------
    assert logs.contains('convertible: 3, blocked: 1'),
            "report must summarise 3 convertible + 1 blocked"
    assert logs =~ /Convertible[\s\S]*clean-leaf/,     "clean-leaf must be listed as convertible"
    assert logs =~ /Convertible[\s\S]*with-dep/,       "with-dep must be listed as convertible"
    assert logs =~ /Convertible[\s\S]*with-threshold/, "with-threshold must be listed as convertible"

    // dependenciesThreshold reconciliation: anchor takes the MAX across ALL OpenL projects.
    // with-threshold declares 4, blocked-config (kept) declares 5 → anchor max must be 5.
    assert logs.contains('<dependenciesThreshold>5</dependenciesThreshold>'),
            "anchor checklist must set dependenciesThreshold to the max (5) across all projects"
    assert !logs.contains('<dependenciesThreshold>4</dependenciesThreshold>'),
            "the lower per-project threshold (4) must not be the reported anchor value"

    // Hoist report: only the provided commons-lang3 goes to the anchor; the optional jar (commons-text)
    // is routed to with-dep/rules.xml instead.
    assert logs.contains('<artifactId>commons-lang3</artifactId>'),
            "the provided dep must be reported in the anchor <dependencies> hoist list"
    assert logs.contains('<scope>provided</scope>'),
            "the hoisted dep must preserve its provided scope"

    // Blocked reason surfaced for the user.
    assert logs =~ /blocked-config:.*skipITs/,
            "blocked-config must be reported with the skipITs reason"

    // Deletion confirmation lines.
    assert logs =~ /Deleted .*clean-leaf.*pom\.xml/,     "must log the clean-leaf pom deletion"
    assert logs =~ /Deleted .*with-dep.*pom\.xml/,       "must log the with-dep pom deletion"
    assert logs =~ /Deleted .*with-threshold.*pom\.xml/, "must log the with-threshold pom deletion"

    // ---------------------------------------------------------------------------------------
    // Anchor auto-edit — the anchor pom.xml is rewritten in place.
    // ---------------------------------------------------------------------------------------
    def anchor = new File(folder, 'pom.xml').text

    // openl-maven-plugin keeps extensions=true and gains the max threshold (5) as configuration.
    assert anchor =~ /(?s)<artifactId>openl-maven-plugin<\/artifactId>[\s\S]*?<extensions>true<\/extensions>/,
            "anchor must declare openl-maven-plugin with <extensions>true</> in <build><plugins>"
    assert anchor.contains('<dependenciesThreshold>5</dependenciesThreshold>'),
            "anchor plugin <configuration> must carry the reconciled max dependenciesThreshold (5)"

    // Provided dep hoisted to the anchor <dependencies>; the optional jar must NOT be there (it went to rules.xml).
    assert anchor =~ /(?s)<dependencies>[\s\S]*<artifactId>commons-lang3<\/artifactId>[\s\S]*<scope>provided<\/scope>/,
            "the provided commons-lang3 dep must be hoisted into the anchor <dependencies>"
    assert !anchor.contains('commons-text'),
            "the optional jar (commons-text) must NOT be on the anchor — it belongs in with-dep/rules.xml"

    // Converted modules dropped from <modules>; the blocked one stays.
    assert !(anchor =~ /<module>clean-leaf<\/module>/),     "clean-leaf must be removed from anchor <modules>"
    assert !(anchor =~ /<module>with-dep<\/module>/),       "with-dep must be removed from anchor <modules>"
    assert !(anchor =~ /<module>with-threshold<\/module>/), "with-threshold must be removed from anchor <modules>"
    assert anchor =~ /<module>blocked-config<\/module>/,    "blocked-config must remain in anchor <modules>"

    assert logs =~ /Edited .*openl-migrate-pomless.*pom\.xml/, "must log the anchor pom edit"
    assert logs =~ /Updated .*with-dep.*rules\.xml.*merged 1 <mavenArtifact>.*appended 1 new entry/,
            "must log merging the OpenL sibling's GAV into the existing <name> entry AND appending the jar dep"

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
