import java.util.regex.Pattern

try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // ---------------------------------------------------------------------------------------
    // Reactor-level expectations: the aggregator itself passes, every scenario module fails.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('[INFO] BUILD FAILURE') }
    assert lines.any { it =~ /^\[INFO\] Negative tests \.+ SUCCESS/ }
    [
            'EPBDS-12729 Tests failures',
            'Compilation error in OpenL Sources',
            'Compilation error in OpenL Tests',
            'Data Binding Negative Test',
            'OpenL Jar Negative Test',
            'Exceeding default lib threshold',
            'Property Validation',
            'OpenAPI Verify Negative Test',
            'Tests failures',
            'Tests separated',
            'Missed Annotation Template Class (Negative)',
    ].each { name ->
        assert lines.any { it =~ /^\[INFO\] ${Pattern.quote(name)} \.+ FAILURE/ }, "Module '${name}' must fail"
    }

    // Slice the shared log into per-module sections for the checks which are not unique
    // across modules. Lines after the reactor summary belong to no module.
    def sections = [:].withDefault { [] }
    def current = ''
    for (line in lines) {
        if (line.contains('[INFO] Reactor Summary')) {
            current = ''
        }
        def m = line =~ /\[INFO\] -+< org\.openl\.internal:([\w.-]+) >-+/
        if (m.find()) {
            current = m.group(1)
        }
        if (current) {
            sections[current] << line
        }
    }

    // ---------------------------------------------------------------------------------------
    // EPBDS-12729-openl-tests-failure: failures of test tables with the error() expectations.
    // ---------------------------------------------------------------------------------------
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

    // Check summary for failed tests
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

    assert sections['EPBDS-12729-openl-tests-failure'].any { it =~ /Run tests using \d+ threads/ }

    // ---------------------------------------------------------------------------------------
    // openl-compilation-error-in-sources, openl-compilation-error-in-tests: the compilation
    // must break the build; asserted by the reactor summary check above.
    // ---------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------
    // openl-databinding-negative: a mixin class absent on the classpath fails the verification.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('Caused by: java.lang.ClassNotFoundException: mixin.PolicyMixin') }
    assert lines.any { it.contains("Verification is failed for 'org.openl.internal:openl-databinding-negative' artifact.") }

    // ---------------------------------------------------------------------------------------
    // openl-jar-verify-negative: the built jar does not pass the deployment verification.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('Verification is failed for \'org.openl.internal:openl-jar-verify-negative\' artifact.') }

    // ---------------------------------------------------------------------------------------
    // openl-lib-threshold-default: threadCount=none keeps the test run single-threaded,
    // so no parallel run marker is printed by this module.
    // ---------------------------------------------------------------------------------------
    assert !sections['openl-lib-threshold-default'].any { it =~ /Run tests using \d+ threads/ }

    // ---------------------------------------------------------------------------------------
    // openl-maven-property-validation: a duplicated table property value fails the build.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('Found non-unique value \'test1\' for table property \'id\'.') }

    // ---------------------------------------------------------------------------------------
    // openl-openapi-verify-negative: the rules do not match the declared OpenAPI schema.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('OpenAPI Reconciliation: Unexpected method \'Hello2\' is found for path \'/Hello2\'.') }
    assert lines.any { it.contains('OpenAPI Reconciliation: Expected method is not found for path \'/Hello\'.') }

    // ---------------------------------------------------------------------------------------
    // openl-tests-failure: failed OpenL tests break the build, and all report formats are written.
    // ---------------------------------------------------------------------------------------
    def testsFailure = sections['openl-tests-failure']

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

    // Check summary for failed tests
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

    // Check GreetingTest (junit4 console report format; scoped to the module section because
    // the same format is printed by the other test-running modules of this reactor)
    assert testsFailure.any { it =~ /Tests run: 5, Failures: 2, Errors: 0. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert testsFailure.any { it =~ /  Test case: #3. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert testsFailure.any { it =~ /  Test case: #5. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }

    assert testsFailure.any { it =~ /Tests run: 4, Failures: 2, Errors: 1. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert testsFailure.any { it =~ /  Test case: #1. Time elapsed: (< )?0.00\d sec. <<< FAILURE/ }
    assert testsFailure.any { it.contains('    Expected: <Ooops> but was: <3>') }
    assert testsFailure.any { it.contains('     Expected: <bar> but was: <Ooops>') }
    assert testsFailure.any { it =~ /  Test case: #4. Time elapsed: (< )?0.00\d sec. <<< ERROR/ }
    assert testsFailure.any { it.contains('   Error: org.openl.exception.OpenLRuntimeException') }

    // Checks output of arrays
    assert lines.any { it.contains('[INFO]     Expected: <[Complex{ text=A codes=[1] children=null }]> but was: <[Complex{ text=B codes=[1, 2] children=null }]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1, 2, 3]> but was: <[1, null, 3]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1.0, 2.58, 3.0]> but was: <[1.0, 2.5, 3.0]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1.0, 2.7, 3.3]> but was: <null>') }
    assert lines.any { it.contains('[INFO]     Expected: <[3.3, 4.6]> but was: <[1.0]>') }
    assert lines.any { it.contains('[INFO]     Expected: <[1.7]> but was: <null>') }

    // Check total tests statistics
    assert testsFailure.any { it.contains('Tests in error:') }
    assert lines.any { it.contains('  EPBDS-8339.whenExpectCorrectResult#4 java.lang.ArithmeticException') }
    assert lines.any { it.contains('  EPBDS-8339.whenExpectError#4 java.lang.ArithmeticException') }

    assert lines.any { it.contains('Total tests run: 59, Failures: 28, Errors: 2') }

    assert testsFailure.any { it =~ /Run tests using \d+ threads/ }

    // Both junit4 and xlsx reports are written for every executed test table
    def reports = new File(folder, 'openl-tests-failure/target/openl-test-reports')
    [
            'Rules With Error.TryFailedGreetingTest',
            'Rules With Error.TryGreetingTest',
            'Simple Rules.GreetingSuccessful1',
            'Simple Rules.GreetingSuccessful2',
            'Simple Rules.GreetingTest',
            'Spreadsheets.calcTest',
            'EPBDS-8339.whenExpectCorrectResult',
            'EPBDS-8339.whenExpectError',
            'Complex Comparison.Fail',
            'Complex Comparison.Fail2',
            'Complex Comparison.Fail3',
            'Complex Comparison.Pass',
            'Complex Comparison.Pass2',
            'Complex Comparison.Pass3',
    ].each { test ->
        assert new File(reports, "TEST-OpenL.${test}.xml").exists()
        assert new File(reports, "TEST-OpenL.${test}.xlsx").exists()
    }

    // ---------------------------------------------------------------------------------------
    // openl-tests-separated: failures of tests kept in a separate directory break the build.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('Simple Rules.GreetingTest#2 expected: <Good Afternoon, World!!!> but was <Good Afternoon, World!>') }
    assert lines.any { it.contains('tests.SeparateGreetingTest#1 expected: <Good Evening, World!!!> but was <Good Evening, World!>') }

    assert new File(folder, 'openl-tests-separated/target/openl-test-reports/TEST-OpenL.Simple Rules.GreetingTest.xml').exists()
    assert new File(folder, 'openl-tests-separated/target/openl-test-reports/TEST-OpenL.tests.SeparateGreetingTest.xml').exists()

    // ---------------------------------------------------------------------------------------
    // openl-verify-missed-annotation-template-class-negative: the annotation template class
    // declared in rules-deploy.xml is absent on the classpath.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('Failed to load or apply annotation template class \'MustNotBeFoundInClassloader\'.') }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
