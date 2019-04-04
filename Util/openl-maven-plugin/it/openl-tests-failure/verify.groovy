try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // Check that all tests are run
    assert lines.any { it.contains('Running GreetingTest from the module Simple Rules') }
    assert lines.any { it.contains('Running GreetingSuccessful1 from the module Simple Rules') }
    assert lines.any { it.contains('Running GreetingSuccessful2 from the module Simple Rules') }
    assert lines.any { it.contains('Running TryGreetingTest from the module Rules With Error') }
    assert lines.any { it.contains('Running TryFailedGreetingTest from the module Rules With Error') }
    assert lines.any { it.contains('Running calcTest from the module Spreadsheets') }
    assert lines.any { it.contains('Running whenExpectCorrectResult from the module EPBDS-8339') }
    assert lines.any { it.contains('Running whenExpectError from the module EPBDS-8339') }

    // Check summary for failed test s
    assert lines.any { it.contains('Rules With Error.TryGreetingTest#4 expected: <Good Evening, World!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#1 expected: <Good Morning!> but was <Good Morning, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#2 expected: <Good Afternoon!> but was <Good Afternoon, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#3 expected: <Good Evening!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTest#4 expected: <Good Night!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Simple Rules.GreetingTest#3 expected: <Good Evening, World!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Simple Rules.GreetingTest#5 expected: <Good Night, World!> but was <null>') }
    assert lines.any { it.contains('Spreadsheets.calcTest#2 field $Value$Sum expected: <1> but was <11>, field $Value$Increment.$Value$IncA expected: <2> but was <6>, field $Value$Increment.$Value$IncB expected: <3> but was <7>') }
    assert lines.any { it.contains('Spreadsheets.calcTest#3 field $Value$Increment.$Value$IncA expected: <6> but was <4>') }

    // Check GreetingTest 
    assert lines.any { it.contains('Tests run: 5, Failures: 2, Errors: 0. Time elapsed: < 0.001 sec. <<< FAILURE!') }
    assert lines.any { it.contains('  Test case: #3. Time elapsed: < 0.001 sec. <<< FAILURE!') }
    assert lines.any { it.contains('  Test case: #5. Time elapsed: < 0.001 sec. <<< FAILURE!') }

    assert lines.any { it =~ /Tests run: 4, Failures: 2, Errors: 1. Time elapsed: (< )?0.00\d sec. <<< FAILURE!/ }
    assert lines.any { it =~ /  Test case: #1. Time elapsed: (< )?0.00\d sec. <<< FAILURE!/ }
    assert lines.any { it.contains('    Expected: <Ooops> but was: <3>') }
    assert lines.any { it =~ /  Test case: #3. Time elapsed: (< )?0.00\d sec. <<< FAILURE!/ }
    assert lines.any { it.contains('     Expected: <bar> but was: <Ooops>') }
    assert lines.any { it =~ /  Test case: #4. Time elapsed: (< )?0.00\d sec. <<< ERROR!/ }
    assert lines.any { it.contains('   Error: org.openl.exception.OpenLRuntimeException') }

    // Check total tests statistics
    assert lines.any { it.contains('Tests in error:') }
    assert lines.any { it.contains('  EPBDS-8339.whenExpectCorrectResult#4 java.lang.ArithmeticException') }
    assert lines.any { it.contains('  EPBDS-8339.whenExpectError#4 java.lang.ArithmeticException') }

    assert lines.any { it.contains('Total tests run: 31, Failures: 13, Errors: 2') }

    assert lines.any { it =~ /Run tests using \d+ threads/ }

    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryFailedGreetingTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryGreetingTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful1.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful2.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Spreadsheets.calcTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectCorrectResult.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectError.xml").exists();

    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryFailedGreetingTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryGreetingTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful1.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful2.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Spreadsheets.calcTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectCorrectResult.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectError.xlsx").exists();

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}