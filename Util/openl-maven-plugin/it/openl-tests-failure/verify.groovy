try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // Check that all tests are run
    assert lines.any { it.contains("Running 'GreetingTest' from module 'Simple Rules'...") }
    assert lines.any { it.contains("Running 'GreetingSuccessful1' from module 'Simple Rules'...") }
    assert lines.any { it.contains("Running 'GreetingSuccessful2' from module 'Simple Rules'...") }
    assert lines.any { it.contains("Running 'TryGreetingTest' from module 'Rules With Error'...") }
    assert lines.any { it.contains("Running 'TryFailedGreetingTest' from module 'Rules With Error'...") }
    assert lines.any { it.contains("Running 'calcTest' from module 'Spreadsheets'...") }
    assert lines.any { it.contains("Running 'whenExpectCorrectResult' from module 'EPBDS-8339'...") }
    assert lines.any { it.contains("Running 'whenExpectError' from module 'EPBDS-8339'...") }
    assert lines.any { it.contains("Running 'Pass' from module 'Complex Comparison'...") }
    assert lines.any { it.contains("Running 'Fail' from module 'Complex Comparison'...") }
    assert lines.any { it.contains("Running 'Pass2' from module 'Complex Comparison'...") }
    assert lines.any { it.contains("Running 'Fail2' from module 'Complex Comparison'...") }
    assert lines.any { it.contains("Running 'Pass3' from module 'Complex Comparison'...") }
    assert lines.any { it.contains("Running 'Fail3' from module 'Complex Comparison'...") }

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
    assert lines.any { it -> it.contains('Complex Comparison.Fail#1 expected: <[1, 2, 3]> but was <null>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail#2 expected: <[1, 1, 1]> but was <[1]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail#3 expected: <null> but was <[1, 2]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail#4 expected: <[1, 2, 3]> but was <[1, null, 3]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail#5 expected: <[1, null, null, 4]> but was <null>') }
    assert lines.any { it -> it.contains('Complex Comparison.Pass3#3 expected: <[Complex{ text=B codes=[1, 2] children=null }, Complex{ text=C codes=[1, 2, 3] children=null }]> but was <[Complex{ text=B codes=[1, 2] children=null }]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail3#1 expected: <[Complex{ text=D codes=[5, 7] children=[Complex{ text=B codes=[1, 2] children=null }, Complex{ text=C codes=[1, 2, 3] children=null }] }]> but was <null>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail3#2 expected: <[Complex{ text=A codes=[1] children=null }, Complex{ text=B codes=[1, 2] children=null }]> but was <[Complex{ text=A codes=[1] children=null }]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail3#3 expected: <[Complex{ text=A codes=[1] children=null }]> but was <[Complex{ text=B codes=[1, 2] children=null }]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail3#4 expected: <[Complex{ text=A codes=[1] children=null }, Complex{ text=B codes=[1, 2] children=null }]> but was <[Complex{ text=B codes=[1, 2] children=null }, Complex{ text=A codes=[1] children=null }]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail3#5 expected: <null> but was <[Complex{ text=A codes=[1] children=null }, Complex{ text=B codes=[1, 2] children=null }, Complex{ text=C codes=[1, 2, 3] children=null }, Complex{ text=D codes=[5, 7] children=[Complex{ text=B codes=[1, 2] children=null }, Complex{ text=C codes=[1, 2, 3] children=null }] }]>') }
    assert lines.any { it -> it.contains('EPBDS-8339.whenExpectCorrectResult#2 expected: <2> but was <3>') }
    assert lines.any { it -> it.contains('EPBDS-8339.whenExpectCorrectResult#3 expected: <3> but was <Ooops>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail2#1 expected: <[1.0, 2.7, 3.3]> but was <null>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail2#2 expected: <[3.3, 4.6]> but was <[1.0]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail2#3 expected: <[1.0, 2.58, 3.0]> but was <[1.0, 2.5, 3.0]>') }
    assert lines.any { it -> it.contains('Complex Comparison.Fail2#4 expected: <[1.7]> but was <null>') }
    assert lines.any { it -> it.contains('EPBDS-8339.whenExpectError#1 expected: <Ooops> but was <3>') }
    assert lines.any { it -> it.contains('EPBDS-8339.whenExpectError#3 expected: <bar> but was <Ooops>') }

    // Check GreetingTest
    assert lines.any { it =~ /Tests run: 5, Failures: 2, Errors: 0. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert lines.any { it =~ /  Test case: #3. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert lines.any { it =~ /  Test case: #5. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }

    assert lines.any { it =~ /Tests run: 4, Failures: 2, Errors: 1. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert lines.any { it =~ /  Test case: #1. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert lines.any { it.contains('    Expected: <Ooops> but was: <3>') }
    assert lines.any { it =~ /  Test case: #3. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert lines.any { it.contains('     Expected: <bar> but was: <Ooops>') }
    assert lines.any { it =~ /  Test case: #4. Time elapsed: (< )?0.00\d sec. <<< ERROR/ }
    assert lines.any { it.contains('   Error: org.openl.exception.OpenLRuntimeException') }

    // Checks output of arrays
    assert lines.any { it.contains('[INFO]     Expected: <[Complex{ text=A codes=[1] children=null }]> but was: <[Complex{ text=B codes=[1, 2] children=null }]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1, 2, 3]> but was: <[1, null, 3]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1.0, 2.58, 3.0]> but was: <[1.0, 2.5, 3.0]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1.0, 2.7, 3.3]> but was: <null>') }
    assert lines.any { it.contains('[INFO]     Expected: <[3.3, 4.6]> but was: <[1.0]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1.7]> but was: <null>') }

    // Check total tests statistics
    assert lines.any { it.contains('Tests in error:') }
    assert lines.any { it.contains('  EPBDS-8339.whenExpectCorrectResult#4 java.lang.ArithmeticException') }
    assert lines.any { it.contains('  EPBDS-8339.whenExpectError#4 java.lang.ArithmeticException') }

    assert lines.any { it.contains('Total tests run: 59, Failures: 28, Errors: 2') }

    assert lines.any { it =~ /Run tests using \d+ threads/ }

    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryFailedGreetingTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryGreetingTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful1.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful2.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Spreadsheets.calcTest.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectCorrectResult.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectError.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Fail.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Fail2.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Fail3.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Pass.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Pass2.xml").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Pass3.xml").exists();

    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryFailedGreetingTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Rules With Error.TryGreetingTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful1.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingSuccessful2.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Spreadsheets.calcTest.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectCorrectResult.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.EPBDS-8339.whenExpectError.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Fail.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Fail2.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Fail3.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Pass.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Pass2.xlsx").exists();
    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Complex Comparison.Pass3.xlsx").exists();

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}