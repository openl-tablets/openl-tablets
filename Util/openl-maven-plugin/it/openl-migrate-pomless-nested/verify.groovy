try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    // ---------------------------------------------------------------------------------------
    // The OpenL project (rating/leaf) is nested under the 'rating' submodule aggregator. Because
    // 'rating/pom.xml' is a pure pass-through (only <parent>+GAV+<modules>, no build/profiles/
    // properties/etc.), openl:pomless COLLAPSES it: 'rating/pom.xml' is deleted and the
    // leaf reanchors to the top-level pom with <flattenGroupId>true</> so its installed coordinates
    // stay stable across the collapse.
    // ---------------------------------------------------------------------------------------

    // leaf converted: its pom.xml deleted, rules.xml kept.
    assert !new File(folder, 'rating/leaf/pom.xml').exists(),
            "rating/leaf/pom.xml must be deleted (cleanly convertible)"
    assert new File(folder, 'rating/leaf/rules.xml').exists(),
            "rating/leaf/rules.xml must remain — only the pom is removed"

    // rating/pom.xml is a pure pass-through aggregator → collapsed and deleted.
    assert !new File(folder, 'rating/pom.xml').exists(),
            "rating/pom.xml is a pass-through aggregator → must be deleted"

    // Top-level becomes THE anchor: gains <flattenGroupId>true</> (so the leaf's installed groupId
    // stays stable across the collapse) and drops the collapsed 'rating' from <modules>.
    def rootPom = new File(folder, 'pom.xml').text
    assert rootPom =~ /(?s)<artifactId>openl-maven-plugin<\/artifactId>[\s\S]*?<extensions>true<\/extensions>/,
            "the top-level (now the single collapse anchor) must declare openl-maven-plugin <extensions>true</>"
    assert rootPom =~ /(?s)<configuration>[\s\S]*?<flattenGroupId>true<\/flattenGroupId>/,
            "the top-level anchor must carry <flattenGroupId>true</> because pass-throughs were collapsed under it"
    assert !(rootPom =~ /<module>rating<\/module>/),
            "the top-level must drop the collapsed 'rating' from <modules>"

    assert logs =~ /Edited .*openl-migrate-pomless-nested[\/\\]pom\.xml/,
            "must log editing the top-level (collapse anchor) pom"
    assert logs =~ /Deleted \(pass-through\) .*[\/\\]rating[\/\\]pom\.xml/,
            "must log the collapse of rating/pom.xml"
    assert logs =~ /Anchor 'openl-migrate-pomless-nested'/,
            "the migration plan must name the top-level as the (collapse) anchor"
    assert logs =~ /pass-through aggregators collapsed: 1/,
            "the plan summary must record one pass-through collapsed"

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
