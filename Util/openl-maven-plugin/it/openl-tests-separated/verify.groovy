try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Simple Rules.GreetingTest#2 expected: <Good Afternoon, World!!!> but was <Good Afternoon, World!>')
    assert logs.contains('tests.SeparateGreetingTest#1 expected: <Good Evening, World!!!> but was <Good Evening, World!>')

    assert new File(basedir, "target/openl-test-reports/TEST-OpenL-Simple Rules-GreetingTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL-tests-SeparateGreetingTest.xml").exists();

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}