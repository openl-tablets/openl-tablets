try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // Check that all tests are run
    assert lines.any { it.contains('Running GreetingTest from the module Simple Rules') }
    assert lines.any { it.contains('Running GreetingSuccessful1 from the module Simple Rules') }
    assert lines.any { it.contains('Running GreetingSuccessful2 from the module Simple Rules') }
    assert lines.any { it.contains('Running TryGreetingTest from the module Rules With Error') }
    assert lines.any { it.contains('Running TryFailedGreetingTest from the module Rules With Error') }

    // Check summary for failed test s
    assert lines.any { it.contains('Rules With Error.TryGreetingTest#4 expected: <Good Evening, World!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#1 expected: <Good Morning!> but was <Good Morning, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#2 expected: <Good Afternoon!> but was <Good Afternoon, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#3 expected: <Good Evening!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#4 expected: <Good Night!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Simple Rules.GreetingTest#3 expected: <Good Evening, World!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Simple Rules.GreetingTest#5 expected: <Good Night, World!> but was <null>') }

    // Check summary for tests in error
    assert lines.any { it.contains('Rules With Error.TryGreetingTest#2 java.lang.ArithmeticException') }

    // Check total tests statistics
    assert lines.any { it.contains('Total tests run: 20, Failures: 7, Errors: 1') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}