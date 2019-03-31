/**
 * Created Nov 9, 2006
 */
package org.openl.module;

import junit.framework.TestCase;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.binding.impl.BindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ModuleTest extends TestCase {

    /**
     * Attributes of this class are referenced in expressions from Person
     * context
     */
    public class Address {
        String street;

        String zip;

        String city;

        public String getCity() {
            return city;
        }

        public String getStreet() {
            return street;
        }

        public String getZip() {
            return zip;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }
    }

    /**
     * Data context class for arithmetic expressions
     */
    public class Order {
        int quantity;

        double price;

        public double getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * Sample data model to use in expressions Person is container, contains one
     * address object
     */
    public class Person {
        String name;

        int age;

        Address address;

        public Address getAddress() {
            return address;
        }

        public int getAge() {
            return age;
        }

        public String getName() {
            return name;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Sample assert expressions in OGNL and OpenL. Note the syntax difference,
     * and "context." prefix within OpenL
     */
    public final static String OGNL_EXPR = "address.zip == 10001";

    public final static String OPENL_EXPR = "context.address.zip.equals(\"10001\")";

    public final static String NEG_OGNL_EXPR = "address.zip != 90210";

    public final static String NEG_OPENL_EXPR = "!context.address.zip.equals(\"90210\")";

    /**
     * Sample get expression in OGNL and OpenL. Note "context." prefix within
     * OpenL
     */
    public final static String OGNL_GET_ADDRESS = "address";

    public final static String OPENL_GET_ADDRESS = "context.address";

    /**
     * Sample arithmetic expressions
     */
    public final static String MATH_OGNL = "10 + price * quantity / 1.05";

    public final static String MATH_OPENL = "10 + context.price * context.quantity / 1.05";

    /**
     * Data context for expressions
     */
    private Person data;

    /**
     * Data context for math expressions
     */
    private Order order;

    private double orderValue;

    /**
     * Execute boolean OpenL expression
     *
     * @param context
     * @param expr
     * @return
     */
    private boolean executeBooleanOpenLExprression(Object context, String expr) {
        IOpenClass retType = JavaOpenClass.BOOLEAN;
        Boolean res = (Boolean) executeOpenLExprression(context, expr, retType);
        return res.booleanValue();
    }

    /**
     * Execute specified OpenL expression within given context object. Note:
     * context object must be refence by "contex." pefrix from expressions
     *
     * @param context context obj
     * @param expr expression string
     * @param retType OpenL return type of expression
     * @return
     */
    private Object executeOpenLExprression(Object context, String expr, IOpenClass retType) {
        IOpenSourceCodeModule src = new StringSourceCodeModule(expr, null);
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);

        JavaOpenClass openClass = JavaOpenClass.getOpenClass(context.getClass());
        IMethodSignature signature = new MethodSignature(new ParameterDeclaration(openClass, "context"));

        OpenMethodHeader methodHeader = new OpenMethodHeader("foo", retType, signature, null);

        BindingContext cxt = new BindingContext(op.getBinder(), null, op);

        IOpenMethod method = OpenLManager.makeMethod(op, src, methodHeader, cxt);

        IRuntimeEnv env = op.getVm().getRuntimeEnv();

        return method.invoke(null, new Object[] { context }, env);
    }

    /**
     * Execute OpenL expression which returns object
     *
     * @param context
     * @param expr
     * @return
     */
    private Object executeOpenLGetExpression(Object context, String expr) {
        IOpenClass retType = JavaOpenClass.OBJECT;
        return executeOpenLExprression(context, expr, retType);
    }

    /**
     * 1) You don't have to specify return type(use JavaOpenClass.VOID instead,
     * in this case openl returns the value of the last expression) 2) New OpenL
     * bex grammar can access attributes of the parameters(actually you can even
     * regulate the depth of the search, if nested attributes need to be
     * accessed, the only limitation is that the name has to be unique in the
     * context, otherwise, the chain syntax still is required)
     *
     * @see http://openl-tablets.sourceforge.net/bex505.shtml
     *
     * 3) Once method is created it can be used multiple times(the instance of
     * IRuntimeEnv need to be created each time or once per thread, but it does
     * not have significant performance overhead)
     *
     */

    private Object executeOpenLOGNLExprression(Object context, String expr) {
        IOpenSourceCodeModule src = new StringSourceCodeModule(expr, null);
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);

        JavaOpenClass openClass = JavaOpenClass.getOpenClass(context.getClass());
        IMethodSignature signature = new MethodSignature(new ParameterDeclaration(openClass, "context"));

        OpenMethodHeader methodHeader = new OpenMethodHeader("foo", JavaOpenClass.VOID, signature, null);

        BindingContext cxt = new BindingContext(op.getBinder(), null, op);

        IOpenMethod method = OpenLManager.makeMethod(op, src, methodHeader, cxt);

        IRuntimeEnv env = op.getVm().getRuntimeEnv();

        return method.invoke(null, new Object[] { context }, env);
    }

    private IOpenMethod makeMethod(ModuleOpenClass module, String expr, IOpenClass retType, OpenL op) {
        IOpenSourceCodeModule src = new StringSourceCodeModule(expr, null);

        IMethodSignature signature = IMethodSignature.VOID;

        OpenMethodHeader methodHeader = new OpenMethodHeader("foo", retType, signature, null);

        BindingContext cxt = new BindingContext(op.getBinder(), null, op);

        ModuleBindingContext moduleContext = new ModuleBindingContext(cxt, module);

        IOpenMethod method = OpenLManager.makeMethod(op, src, methodHeader, moduleContext);

        return method;
    }

    /*
     * (non-Javadoc)
     *
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() {
        Person person = new Person();
        person.setName("John Smith");
        person.setAge(21);

        Address address = new Address();
        address.setStreet("5th avenue 123");
        address.setZip("10001");
        address.setCity("New York");
        person.setAddress(address);
        data = person;

        order = new Order();
        order.setQuantity(10);
        order.setPrice(1.5d);

        // expected value for order
        orderValue = 10 + order.getPrice() * order.getQuantity() / 1.05;
    }

    public void testModule() {
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);

        ModuleOpenClass module = new ModuleOpenClass("ZZZ", op);

        DynamicObjectField field = new DynamicObjectField(module, "address", JavaOpenClass.getOpenClass(Address.class));

        module.addField(field);

        String methodText = "address.zip.equals(\"10001\")";

        IOpenMethod m1 = makeMethod(module, methodText, JavaOpenClass.BOOLEAN, op);

        module.addMethod(m1);

        // /INVOKE

        long start = System.currentTimeMillis();

        int N = 1000;

        Object res = null;
        for (int i = 0; i < N; ++i) {
            IRuntimeEnv env = op.getVm().getRuntimeEnv();
            Object instance = module.newInstance(env);

            field.set(instance, data.address, env);

            res = m1.invoke(instance, new Object[] {}, env);
        }

        long end = System.currentTimeMillis();

        double run = ((double) (end - start)) / N;

        System.out.println("Result:" + res + ". Invoke time = " + run + " ms");

    }

    /**
     * Test sample "assert" expressions is OpenL
     */
    public void testOpenL() {
        boolean b;
        long t = System.currentTimeMillis();
        b = executeBooleanOpenLExprression(data, OPENL_EXPR);
        assertTrue(b);

        b = executeBooleanOpenLExprression(data, NEG_OPENL_EXPR);
        assertTrue(b);
        System.out.println(System.currentTimeMillis() - t);
    }

    /**
     * Test sample "get" expression in OpenL
     */
    public void testOpenLGet() {
        Object obj = executeOpenLGetExpression(data, OPENL_GET_ADDRESS);
        assertTrue(obj == data.getAddress());
    }

    /**
     * Test sample arithemtic expression in OpenL
     */
    public void testOpenLMath() {
        /*
         * This invocation does not work with primitive values, e.g. in
         * arithemtic expressions
         */
        // Object obj = executeOpenLGetExpression(order, MATH_OPENL);
        // XXX: workaround: have to specify expected return type for arithmetic
        // expressions - not good for BLS engine
        Object obj = executeOpenLExprression(order, MATH_OPENL, JavaOpenClass.getOpenClass(Double.class)); // <--
                                                                                                            // problematic,
                                                                                                            // we
                                                                                                            // have
                                                                                                            // to
                                                                                                            // know
        // expresion return type before we invoke it
        // thats something we dont know (and can't) in BLS
        double value = ((Double) obj).doubleValue();
        assertEquals(orderValue, value, 0.00001);
    }

    public void testOpenLOGNLMath() {
        /*
         * This invocation does not work with primitive values, e.g. in
         * arithemtic expressions
         */
        // Object obj = executeOpenLGetExpression(order, MATH_OPENL);
        // XXX: workaround: have to specify expected return type for arithmetic
        // expressions - not good for BLS engine
        Object obj = executeOpenLOGNLExprression(order, MATH_OGNL); // <--
                                                                    // problematic,
                                                                    // we have
                                                                    // to know
        // expresion return type before we invoke it
        // thats something we dont know (and can't) in BLS
        double value = ((Double) obj).doubleValue();
        assertEquals(orderValue, value, 0.00001);
    }

}
