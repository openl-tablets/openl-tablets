try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Simple Rules.GreetingTest#2 expected: <Good Afternoon, World!!!> but was <Good Afternoon, World!>')
    assert logs.contains('tests.SeparateGreetingTest#1 expected: <Good Evening, World!!!> but was <Good Evening, World!>')

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}