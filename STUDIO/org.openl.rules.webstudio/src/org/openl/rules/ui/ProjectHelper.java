/**
 * Created Mar 24, 2007
 */
package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodDelegator;

/**
 * @author snshor
 *
 */
public final class ProjectHelper {

    private ProjectHelper() {
    }

    public static TestSuiteMethod[] allTesters(IOpenClass openClass) {
        List<TestSuiteMethod> res = new ArrayList<TestSuiteMethod>();
        for (IOpenMethod tester : openClass.getMethods()) {
            if (isTester(tester)) {
                res.add((TestSuiteMethod) tester);
            }
            if (tester instanceof OpenMethodDispatcher && isTester(((OpenMethodDispatcher) tester).getTargetMethod())) {
                res.add((TestSuiteMethod) ((OpenMethodDispatcher) tester).getTargetMethod());
            }
        }

        TestSuiteMethod[] testSuiteMethods = new TestSuiteMethod[res.size()];
        return res.toArray(testSuiteMethods);
    }

    /**
     * Checks if the tester is instance of {@link TestSuiteMethod}, if it has
     * any parameters for testing(see
     * {@link TestSuiteMethod#isRunmethodTestable()}) and if there is no errors
     * in it.
     * 
     * @param tester instance of method that is considered to be a test.
     * @return true if tester is valid {@link TestSuiteMethod}.
     */
    public static boolean isTester(IOpenMethod tester) {
        if (tester instanceof TestSuiteMethod) {
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) tester;
            return !testSuiteMethod.isRunmethod() && testSuiteMethod.isRunmethodTestable() && !testSuiteMethod.getSyntaxNode().hasErrors();

        }
        return false;
    }

    /**
     * Get tests for tested method that have filled rules rows data for testing
     * its functionality. Run methods and tests with empty test cases are not
     * being processed. If you need to get all test methods, including run
     * methods and empty ones, use
     * {@link #isTestForMethod(IOpenMethod, IOpenMethod)}.
     */
    public static IOpenMethod[] testers(IOpenMethod tested) {

        List<IOpenMethod> res = new ArrayList<IOpenMethod>();
        for (IOpenMethod tester : tested.getDeclaringClass().getMethods()) {
            if (isTester(tester) && isTestForMethod(tester, tested)) {
                res.add(tester);
            }

        }

        return res.toArray(new IOpenMethod[res.size()]);
    }

    /**
     * If tester is an instance of {@link TestSuiteMethod} and tested method
     * object in tester is equal to tested we consider tester is test for tested
     * method.
     */
    public static boolean isTestForMethod(IOpenMethod tester, IOpenMethod tested) {
        if (!(tester instanceof TestSuiteMethod)) {
            return false;
        }
        IOpenMethod toTest = ((TestSuiteMethod) tester).getTestedMethod();
        if (toTest == tested) {
            return true;
        }
        if (toTest instanceof OpenMethodDispatcher) {
            if (((OpenMethodDispatcher) toTest).getCandidates().contains(tested)) {
                return true;
            }
        }
        if (tested instanceof MethodDelegator) {
            return isTestForMethod(tester, tested.getMethod());
        }
        if (tested instanceof OpenMethodDispatcher) {
            return isTestForMethod(tester, ((OpenMethodDispatcher) tested).getTargetMethod());
        }
        return false;
    }

    public static String getTestName(IOpenMethod testMethod) {
        IMemberMetaInfo mi = testMethod.getInfo();
        TableSyntaxNode tnode = (TableSyntaxNode) mi.getSyntaxNode();
        return TableSyntaxNodeUtils.getTableDisplayValue(tnode)[INamedThing.SHORT];
    }

    public static String getTestInfo(IOpenMethod testMethod) {
        String info = null;

        if (testMethod instanceof TestSuiteMethod) {
            TestSuiteMethod testSuite = (TestSuiteMethod) testMethod;
            int numberOfTests = testSuite.getNumberOfTests();
            if (testSuite.isRunmethod()) {
                if (numberOfTests < 1) {
                    info = "No runs";
                } else {
                    info = numberOfTests + " runs";
                }
            } else {
                if (numberOfTests < 1) {
                    info = "No test cases";
                } else {
                    info = numberOfTests + " test cases";
                }
            }
        }

        return info;
    }
}
