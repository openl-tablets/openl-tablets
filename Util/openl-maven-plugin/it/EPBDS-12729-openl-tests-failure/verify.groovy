try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // Check that all tests are run
    assert lines.any { it.contains("Running 'error1_test1' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error3_test2' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error1_test3' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error2_test3' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error4_test5' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error2_test1' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error3_test3' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error1_test2' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error3_test1' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error2_test2' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error4_test4' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error2_test4' from module 'Main'...") }
    assert lines.any { it.contains("Running 'error4_test1' from module 'Main'...") }

    // Check summary for failed test s
    assert lines.any { it.contains('Main.error1_test1#1 expected: <Foo bar> but was <foo.bar: Foo bar>') }
    assert lines.any { it.contains('Main.error1_test1#2 expected: <foo bar> but was <foo.bar: Foo bar>') }
    assert lines.any { it.contains('Main.error1_test1#3 expected: <null> but was <foo.bar: Foo bar>') }
    assert lines.any { it.contains('Main.error3_test2#2 field message expected: <null> but was <Foo bar>') }
    assert lines.any { it.contains('Main.error3_test2#3 field message expected: <qwerty> but was <Foo bar>') }
    assert lines.any { it.contains('Main.error1_test3#2 field message expected: <foo bar> but was <Foo bar>') }
    assert lines.any { it.contains('Main.error1_test3#3 field message expected: <foo bar> but was <Foo bar>, field code expected: <bar.foo> but was <foo.bar>') }
    assert lines.any { it.contains('Main.error1_test3#4 field code expected: <bar.foo> but was <foo.bar>') }
    assert lines.any { it.contains('Main.error1_test3#5 field message expected: <null> but was <Foo bar>, field code expected: <null> but was <foo.bar>') }
    assert lines.any { it.contains('Main.error2_test3#2 field code expected: <bar.foo> but was <foo.bar>') }
    assert lines.any { it.contains('Main.error2_test3#3 field code expected: <null> but was <foo.bar>') }
    assert lines.any { it.contains('Main.error4_test5#3 field code expected: <null> but was <foo.bar>, field message expected: <null> but was <Foo bar>') }
    assert lines.any { it.contains('Main.error2_test1#1 expected: <Foo bar> but was <foo.bar: null>') }
    assert lines.any { it.contains('Main.error2_test1#2 expected: <null> but was <foo.bar: null>') }
    assert lines.any { it.contains('Main.error3_test3#1 field code expected: <foo.bar> but was <null>') }
    assert lines.any { it.contains('Main.error1_test2#2 field message expected: <foo bar> but was <Foo bar>') }
    assert lines.any { it.contains('Main.error1_test2#3 field message expected: <null> but was <Foo bar>') }
    assert lines.any { it.contains('Main.error3_test1#1 expected: <Foo bar> but was <null: Foo bar>') }
    assert lines.any { it.contains('Main.error3_test1#2 expected: <null> but was <null: Foo bar>') }
    assert lines.any { it.contains('Main.error2_test2#1 field message expected: <Foo bar> but was <null>') }
    assert lines.any { it.contains('Main.error4_test4#3 expected: <null> but was <foo.bar: Foo bar>') }
    assert lines.any { it.contains('Main.error2_test4#2 field message expected: <foo bar> but was <null>') }
    assert lines.any { it.contains('Main.error2_test4#3 field code expected: <bar.foo> but was <foo.bar>') }
    assert lines.any { it.contains('Main.error2_test4#4 field code expected: <null> but was <foo.bar>') }

    // Check total tests statistics
    assert lines.any { it.contains('Total tests run: 39, Failures: 27, Errors: 0') }

    assert lines.any { it =~ /Run tests using \d+ threads/ }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}