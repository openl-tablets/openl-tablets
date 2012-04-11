/**
 * Created Mar 24, 2007
 */
package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class ProjectHelper {
    
    private static final String TEST_CASES = "test cases";
    private static final String NO = "No";
    private static final String RUNS = "runs";    
    
    public static IOpenMethod[] allTesters(IOpenClass openClass) {
        List<IOpenMethod> res = new ArrayList<IOpenMethod>();
        for (IOpenMethod tester : openClass.getMethods()) {
            if (isTester(tester)) {
                res.add(tester);
            }
        }

        return (IOpenMethod[]) res.toArray(new IOpenMethod[0]);
    }

    public static boolean isMethodRunnedBy(IOpenMethod tested, IOpenMethod runner) {
        if (!(runner instanceof TestSuiteMethod)) {
            return false;
        }
        if (runner == tested) {
            return true;
        }
        IOpenMethod toTest = ((TestSuiteMethod) runner).getTestedMethod();
        return toTest == tested && ((TestSuiteMethod) runner).isRunmethod();
    }

    public static boolean isMethodTestedBy(IOpenMethod tested, IOpenMethod tester) {
        if (!(tester instanceof TestSuiteMethod)) {
            return false;
        }
        IOpenMethod toTest = ((TestSuiteMethod) tester).getTestedMethod();
        return toTest == tested && isTester(tester);
    }

    public static boolean isRunnable(IOpenMethod m) {

        IOpenClass[] par = m.getSignature().getParameterTypes();
        if (par.length == 0) {
            return true;
        }

        // if (isTestable(m))
        // return true;

        return false;
    }

    public static boolean isTestable(IOpenMethod m) {
        return testers(m).length > 0;
    }
    
    /**
     * Checks if the tester is instance of {@link TestSuiteMethod} and if it has any parameters for testing(see 
     * {@link TestSuiteMethod#isRunmethodTestable()}).
     * 
     * @param tester instance of method that is considered to be a test.
     * @return 
     */
    public static boolean isTester(IOpenMethod tester) {
        return (tester instanceof TestSuiteMethod) && ((TestSuiteMethod) tester).isRunmethodTestable();
    }

    public static IOpenMethod[] runners(IOpenMethod tested) {

        List<IOpenMethod> res = new ArrayList<IOpenMethod>();
        for (IOpenMethod runner : tested.getDeclaringClass().getMethods()) {
            if (isMethodRunnedBy(tested, runner)) {
                res.add(runner);
            }

        }

        return res.toArray(new IOpenMethod[0]);
    }
    
    /**
     * Get tests for tested method that have filled rules rows data for testing its functionality. Run methods and tests 
     * with empty test cases are not being processed. 
     * If you need to get all test methods, including run methods and empty ones, use {@link #allTesters(IOpenMethod)}. 
     * @param tested
     * @return
     */
    public static IOpenMethod[] testers(IOpenMethod tested) {

        List<IOpenMethod> res = new ArrayList<IOpenMethod>();
        for (IOpenMethod tester : tested.getDeclaringClass().getMethods()) {
            if (isMethodTestedBy(tested, tester)) {
                res.add(tester);
            }

        }

        return (IOpenMethod[]) res.toArray(new IOpenMethod[0]);
    }
    
    /**
     * gets all the tests for tested method.
     * 
     * @param tested
     * @return all test methods, including tests with test cases, runs with filled runs, tests without cases(empty),
     * runs without any parameters and tests without cases and runs.
     */
    public static IOpenMethod[] allTesters(IOpenMethod tested) {
        List<IOpenMethod> res = new ArrayList<IOpenMethod>();
        for (IOpenMethod tester : tested.getDeclaringClass().getMethods()) {
            if (isTestForMethod(tester,tested)) {
                res.add(tester);
            }
        }
        return (IOpenMethod[]) res.toArray(new IOpenMethod[0]);
    }

    /**
     * If tester is an instance of {@link TestSuiteMethod} and tested method object in tester is equal to tested we 
     * consider tester is test for tested method.
     * 
     * @param tester
     * @param tested
     * @return
     */
    private static boolean isTestForMethod(IOpenMethod tester, IOpenMethod tested) {
        if (!(tester instanceof TestSuiteMethod)) {
            return false;
        }
        IOpenMethod toTest = ((TestSuiteMethod) tester).getTestedMethod();
        return toTest == tested;
    }
    
    public static String createTestName(IOpenMethod testMethod) {
        IMemberMetaInfo mi = testMethod.getInfo();
        TableSyntaxNode tnode = (TableSyntaxNode) mi.getSyntaxNode();

        String name = TableSyntaxNodeUtils.getTableDisplayValue(tnode)[1];
        if (testMethod instanceof TestSuiteMethod) {
           TestSuiteMethod testSuite = (TestSuiteMethod)testMethod;
           if (testSuite.isRunmethod()) {
               if (testSuite.nUnitRuns() < 1) {
                   name += getTestAdditionalInfo(NO, RUNS);
               } else {
                   name += getNumberOfTests(testSuite.nUnitRuns(), RUNS);
               }
           } else {
               if (testSuite.getNumberOfTests() < 1) {
                   name += getTestAdditionalInfo(NO, TEST_CASES);
               } else {
                   name += getNumberOfTests(testSuite.getNumberOfTests(), TEST_CASES);
               }
           }
        }
        
        return name;
    }
    
    private static String getNumberOfTests(int param1, String param2) {        
        return String.format(" (%d %s)", param1, param2);
    }

    private static String getTestAdditionalInfo(String param1, String param2) {
        return String.format(" (%s %s)", param1, param2);
    }

}
