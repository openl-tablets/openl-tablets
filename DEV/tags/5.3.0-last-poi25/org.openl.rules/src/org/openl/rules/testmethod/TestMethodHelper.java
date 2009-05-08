/**
 * Created Jan 2, 2007
 */
package org.openl.rules.testmethod;

import org.openl.base.INamedThing;
import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.testmethod.binding.TestMethodBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;
import org.openl.util.print.Formatter;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */

public class TestMethodHelper {

    class TestMethodOpenClass extends ADynamicClass {

        TestMethodOpenClass() {
            super(null, tableName + "TestClass", DynamicObject.class);
            IOpenClass[] ioc = testedMethod.getSignature().getParameterTypes();
            for (int i = 0; i < ioc.length; i++) {
                String name = testedMethod.getSignature().getParameterName(i);
                IOpenField iof = new DynamicObjectField(this, name, ioc[i]);
                addField(iof);
            }

            IOpenField res = new DynamicObjectField(this, EXPECTED_RESULT_NAME, testedMethod.getType());
            addField(res);
            IOpenField descr = new DynamicObjectField(this, DESCRIPTION_NAME, JavaOpenClass.STRING);
            addField(descr);

        }

        public Object newInstance(IRuntimeEnv env) {
            DynamicObject res = new DynamicObject(this);
            return res;
        }
    }

    public class TestMethodTestAll extends AMethod implements IMemberMetaInfo, IBenchmarkableMethod {

        /**
         * @param header
         */
        public TestMethodTestAll() {
            super(makeHeader());
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.impl.IBenchmarkableMethod#getBenchmarkName()
         */
        public String getBenchmarkName() {
            return "Test " + testedMethod.getName();
        }

        public BindingDependencies getDependencies() {
            return new BindingDependencies();
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return this;
        }

        public int getNumberOfTests() {
            Object testArray = boundNode.getField().getTable().getDataArray();
            DynamicObject[] dd = (DynamicObject[]) testArray;
            return dd.length;
        }

        public String getSourceUrl() {
            return ((TableSyntaxNode) getSyntaxNode()).getUri();
        }

        public ISyntaxNode getSyntaxNode() {
            return boundNode.getSyntaxNode();
        }

        public String[] getTestDescriptions() {
            Object testArray = boundNode.getField().getTable().getDataArray();

            DynamicObject[] dd = (DynamicObject[]) testArray;

            String[] res = new String[dd.length];

            for (int i = 0; i < res.length; i++) {
                String descr = (String) dd[i].getFieldValue(DESCRIPTION_NAME);
                if (descr == null) {

                    String pname = testedMethod.getSignature().getParameterName(0);
                    Object pvalue = dd[i].getFieldValue(pname);
                    descr = Formatter.format(pvalue, INamedThing.REGULAR, new StringBuffer()).toString();
                }
                res[i] = descr;
            }

            return res;

        }

        public IOpenMethod getTested() {
            return getTestedMethod();
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return invokeBenchmark(target, params, env, 1);
        }

        public Object invokeBenchmark(Object target, Object[] params, IRuntimeEnv env, int ntimes) {
            Object testArray = boundNode.getField().get(target, env);

            DynamicObject[] dd = (DynamicObject[]) testArray;

            IOpenClass dclass = getMethodBasedClass();
            IMethodSignature msign = testedMethod.getSignature();
            IOpenClass[] mpars = msign.getParameterTypes();

            TestResult tres = new TestResult(TestMethodHelper.this);
            // Object[] results = new Object[dd.length];

            for (int i = 0; i < dd.length; i++) {
                Object[] mpvals = new Object[mpars.length];
                for (int j = 0; j < mpars.length; j++) {
                    IOpenField f = dclass.getField(msign.getParameterName(j), true);
                    mpvals[j] = f.get(dd[i], env);
                }

                try {
                    Object res = null;

                    for (int j = 0; j < ntimes; j++) {
                        res = testedMethod.invoke(target, mpvals, env);
                    }

                    // if (isRunmethod())
                    // results[i] = res;
                    // else
                    tres.add(dd[i], res, null);
                } catch (Throwable t) {
                    Log.error("Testing " + dd[i], t);
                    tres.add(dd[i], null, t);
                }

            }

            return
            // isRunmethod() ? (Object)results :
            tres;
        }

        public boolean isRunmethod() {
            TableSyntaxNode tsn = (TableSyntaxNode) getSyntaxNode();
            return ITableNodeTypes.XLS_RUN_METHOD.equals(tsn.getType());
        }

        public boolean isRunmethodTestable() {
            Object testArray = boundNode.getField().getTable().getDataArray();

            DynamicObject[] dd = (DynamicObject[]) testArray;

            for (int i = 0; i < dd.length; i++) {
                if (dd[i].containsField(EXPECTED_RESULT_NAME)) {
                    return true;
                }
            }

            return false;
        }

        public int nUnitRuns() {
            return getNumberOfTests();
        }

        /**
         * @param tid
         * @param target
         * @param env
         * @return
         */
        public Object run(int tid, Object target, IRuntimeEnv env, int ntimes) {
            Object testArray = boundNode.getField().get(target, env);

            DynamicObject[] dd = (DynamicObject[]) testArray;

            IOpenClass dclass = getMethodBasedClass();
            IMethodSignature msign = testedMethod.getSignature();
            IOpenClass[] mpars = msign.getParameterTypes();

            // TestResult tres = new TestResult(TestMethodHelper.this);

            Object[] mpvals = new Object[mpars.length];
            for (int j = 0; j < mpars.length; j++) {
                IOpenField f = dclass.getField(msign.getParameterName(j), true);
                mpvals[j] = f.get(dd[tid], env);
            }

            Object res = null;

            for (int i = 0; i < ntimes; i++) {
                res = testedMethod.invoke(target, mpvals, env);
            }

            return res;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.impl.IBenchmarkableMethod#unitName()
         */
        public String[] unitName() {
            return new String[] { "Test Unit", "Test Units" };
        }

    }

    public static final String EXPECTED_RESULT_NAME = "_res_";

    static final String DESCRIPTION_NAME = "_description_";

    IOpenMethod testedMethod;
    IOpenClass methodBasedClass = null;

    TestMethodBoundNode boundNode;

    String tableName;

    public TestMethodHelper(IOpenMethod method, String tableName) {
        testedMethod = method;
        this.tableName = tableName;
    }

    public TestMethodBoundNode getBoundNode() {
        return boundNode;
    }

    public synchronized IOpenClass getMethodBasedClass() {
        if (methodBasedClass == null) {
            methodBasedClass = new TestMethodOpenClass();
        }
        return methodBasedClass;
    }

    public IOpenMethod getTestAll() {
        return new TestMethodTestAll();
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }

    IOpenMethodHeader makeHeader() {
        OpenMethodHeader mh = new OpenMethodHeader(tableName + "TestAll", JavaOpenClass.getOpenClass(TestResult.class),
                IMethodSignature.VOID, testedMethod.getDeclaringClass());
        return mh;
    }

    public void setBoundNode(TestMethodBoundNode boundNode) {
        this.boundNode = boundNode;
    }

}
