package org.openl.rules.ui;

import org.openl.rules.testmethod.TestMethodHelper.TestMethodTestAll;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

import java.util.Iterator;
import java.util.Vector;


/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public class ProjectHelper {
    public static boolean isMethodTestedBy(IOpenMethod tested, IOpenMethod tester) {
        if (!(tester instanceof TestMethodTestAll)) {
            return false;
        }
        IOpenMethod toTest = ((TestMethodTestAll) tester).getTested();
        return (toTest == tested) && isTester(tester);
    }

    public static boolean isMethodRunnedBy(IOpenMethod tested, IOpenMethod runner) {
        if (!(runner instanceof TestMethodTestAll)) {
            return false;
        }
        IOpenMethod toTest = ((TestMethodTestAll) runner).getTested();
        return (toTest == tested) && ((TestMethodTestAll) runner).isRunmethod();
    }

    public static IOpenMethod[] testers(IOpenMethod tested) {
        Vector res = new Vector();
        for (Iterator iter = tested.getDeclaringClass().methods(); iter.hasNext();) {
            IOpenMethod tester = (IOpenMethod) iter.next();
            if (isMethodTestedBy(tested, tester)) {
                res.add(tester);
            }
        }

        return (IOpenMethod[]) res.toArray(new IOpenMethod[0]);
    }

    public static IOpenMethod[] runners(IOpenMethod tested) {
        Vector res = new Vector();
        for (Iterator iter = tested.getDeclaringClass().methods(); iter.hasNext();) {
            IOpenMethod runner = (IOpenMethod) iter.next();
            if (isMethodRunnedBy(tested, runner)) {
                res.add(runner);
            }
        }

        return (IOpenMethod[]) res.toArray(new IOpenMethod[0]);
    }

    public static boolean isRunnable(IOpenMethod m) {
        IOpenClass[] par = m.getSignature().getParameterTypes();
        if (par.length == 0) {
            return true;
        }

//		if (isTestable(m))
//			return true;
        return false;
    }

    public static boolean isTestable(IOpenMethod m) {
        return testers(m).length > 0;
    }

    /**
     * DOCUMENT ME!
     *
     * @param openClass
     *
     * @return
     */
    public static IOpenMethod[] allTesters(IOpenClass openClass) {
        Vector res = new Vector();
        for (Iterator iter = openClass.methods(); iter.hasNext();) {
            IOpenMethod tester = (IOpenMethod) iter.next();
            if (isTester(tester)) {
                res.add(tester);
            }
        }

        return (IOpenMethod[]) res.toArray(new IOpenMethod[0]);
    }

    /**
     * DOCUMENT ME!
     *
     * @param tester
     *
     * @return
     */
    private static boolean isTester(IOpenMethod tester) {
        return (tester instanceof TestMethodTestAll)
            && ((TestMethodTestAll) tester).isRunmethodTestable();
    }
}
