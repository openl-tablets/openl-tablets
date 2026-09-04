/**
 * Created Nov 9, 2006
 */
package org.openl.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.OpenL;
import org.openl.binding.impl.BindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 */
class ModuleTest {
    private final Logger log = LoggerFactory.getLogger(ModuleTest.class);

    /**
     * Attributes of this class are referenced in expressions from Person context
     */
    public static class Address {
        @Getter
        @Setter
        String street;

        @Getter
        @Setter
        String zip;

        @Getter
        @Setter
        String city;
    }

    /**
     * Data context class for arithmetic expressions
     */
    public static class Order {
        @Getter
        @Setter
        int quantity;

        @Getter
        @Setter
        double price;
    }

    /**
     * Sample data model to use in expressions Person is container, contains one address object
     */
    public static class Person {
        @Getter
        @Setter
        String name;

        @Getter
        @Setter
        int age;

        @Getter
        @Setter
        Address address;
    }

    /**
     * Sample assert expressions in OpenL. Note the "context." prefix.
     */
    private static final String OPENL_EXPR = "context.address.zip.equals(\"10001\")";

    private static final String NEG_OPENL_EXPR = "!context.address.zip.equals(\"90210\")";

    /**
     * Sample get expression in OpenL. Note the "context." prefix.
     */
    private static final String OPENL_GET_ADDRESS = "context.address";

    /**
     * Sample arithmetic expressions
     */
    private static final String MATH_OGNL = "10 + price * quantity / 1.05";

    private static final String MATH_OPENL = "10 + context.price * context.quantity / 1.05";

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
    private boolean executeBooleanOpenLExprression(Object context, String expr) throws SyntaxNodeException {
        var retType = JavaOpenClass.BOOLEAN;
        return (Boolean) executeOpenLExprression(context, expr, retType);
    }

    /**
     * Execute specified OpenL expression within given context object. Note: context object must be refence by "contex."
     * pefrix from expressions
     *
     * @param context context obj
     * @param expr    expression string
     * @param retType OpenL return type of expression
     * @return
     */
    private Object executeOpenLExprression(Object context, String expr, IOpenClass retType) throws SyntaxNodeException {
        var src = new StringSourceCodeModule(expr, null);
        OpenL op = OpenL.getInstance();

        JavaOpenClass openClass = JavaOpenClass.getOpenClass(context.getClass());
        var signature = new MethodSignature(new ParameterDeclaration(openClass, "context"));

        var methodHeader = new OpenMethodHeader("foo", retType, signature, null);

        var cxt = new BindingContext(op.getBinder(), null, op);

        IOpenMethod method = OpenLManager.makeMethod(op, src, methodHeader, cxt);

        var env = op.getVm().getRuntimeEnv();

        return method.invoke(null, new Object[]{context}, env);
    }

    /**
     * Execute OpenL expression which returns object
     *
     * @param context
     * @param expr
     * @return
     */
    private Object executeOpenLGetExpression(Object context, String expr) throws SyntaxNodeException {
        var retType = JavaOpenClass.OBJECT;
        return executeOpenLExprression(context, expr, retType);
    }

    /**
     * 1) You don't have to specify return type(use JavaOpenClass.VOID instead, in this case openl returns the value of
     * the last expression) 2) New OpenL bex grammar can access attributes of the parameters(actually you can even
     * regulate the depth of the search, if nested attributes need to be accessed, the only limitation is that the name
     * has to be unique in the context, otherwise, the chain syntax still is required)
     *
     * @see http://openl-tablets.sourceforge.net/bex505.shtml
     * <p>
     * 3) Once method is created it can be used multiple times(the instance of IRuntimeEnv need to be created each
     * time or once per thread, but it does not have significant performance overhead)
     */

    private Object executeOpenLOGNLExprression(Object context, String expr) throws SyntaxNodeException {
        var src = new StringSourceCodeModule(expr, null);
        OpenL op = OpenL.getInstance();

        JavaOpenClass openClass = JavaOpenClass.getOpenClass(context.getClass());
        var signature = new MethodSignature(new ParameterDeclaration(openClass, "context"));

        var methodHeader = new OpenMethodHeader("foo", JavaOpenClass.VOID, signature, null);

        var cxt = new BindingContext(op.getBinder(), null, op);

        IOpenMethod method = OpenLManager.makeMethod(op, src, methodHeader, cxt);

        var env = op.getVm().getRuntimeEnv();

        return method.invoke(null, new Object[]{context}, env);
    }

    private IOpenMethod makeMethod(ModuleOpenClass module, String expr, IOpenClass retType, OpenL op) throws SyntaxNodeException {
        var src = new StringSourceCodeModule(expr, null);

        var signature = IMethodSignature.VOID;

        var methodHeader = new OpenMethodHeader("foo", retType, signature, null);

        var cxt = new BindingContext(op.getBinder(), null, op);

        var moduleBindingContext = new ModuleBindingContext(cxt, module);

        return OpenLManager.makeMethod(op, src, methodHeader, moduleBindingContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see junit.framework.TestCase#setUp()
     */
    @BeforeEach
    void setUp() {
        var person = new Person();
        person.setName("John Smith");
        person.setAge(21);

        var address = new Address();
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

    @Test
    void testModule() throws SyntaxNodeException {
        OpenL op = OpenL.getInstance();

        var module = new ModuleOpenClass("ZZZ", op);

        var field = new DynamicObjectField(module, "address", JavaOpenClass.getOpenClass(Address.class));

        module.addField(field);

        var methodText = "address.zip.equals(\"10001\")";

        var m1 = makeMethod(module, methodText, JavaOpenClass.BOOLEAN, op);

        module.addMethod(m1);

        // /INVOKE

        var start = System.currentTimeMillis();

        var N = 1000;

        Object res = null;
        for (var i = 0; i < N; ++i) {
            var env = op.getVm().getRuntimeEnv();
            var instance = module.newInstance(env);

            field.set(instance, data.address, env);

            res = m1.invoke(instance, new Object[]{}, env);
        }

        var end = System.currentTimeMillis();

        var run = (double) (end - start) / N;

        log.info("TestModule: Result: {}. Elapsed time = {}.", res, run);

    }

    /**
     * Test sample "assert" expressions is OpenL
     */
    @Test
    void testOpenL() throws SyntaxNodeException {
        boolean b;
        var t = System.currentTimeMillis();
        b = executeBooleanOpenLExprression(data, OPENL_EXPR);
        assertTrue(b);

        b = executeBooleanOpenLExprression(data, NEG_OPENL_EXPR);
        assertTrue(b);
        log.info("TestOpenL: Elapsed time = {}.", System.currentTimeMillis() - t);
    }

    /**
     * Test sample "get" expression in OpenL
     */
    @Test
    void testOpenLGet() throws SyntaxNodeException {
        var obj = executeOpenLGetExpression(data, OPENL_GET_ADDRESS);
        assertSame(obj, data.getAddress());
    }

    /**
     * Test sample arithemtic expression in OpenL
     */
    @Test
    void testOpenLMath() throws SyntaxNodeException {
        /*
         * This invocation does not work with primitive values, e.g. in arithemtic expressions
         */
        // Object obj = executeOpenLGetExpression(order, MATH_OPENL);
        // XXX: workaround: have to specify expected return type for arithmetic
        // expressions - not good for BLS engine
        var obj = executeOpenLExprression(order, MATH_OPENL, JavaOpenClass.getOpenClass(Double.class)); // <--
        // problematic,
        // we
        // have
        // to
        // know
        // expresion return type before we invoke it
        // thats something we dont know (and cannot) in BLS
        var value = ((Double) obj);
        assertEquals(orderValue, value, 0.00001);
    }

    @Test
    void testOpenLOGNLMath() throws SyntaxNodeException {
        /*
         * This invocation does not work with primitive values, e.g. in arithemtic expressions
         */
        // Object obj = executeOpenLGetExpression(order, MATH_OPENL);
        // XXX: workaround: have to specify expected return type for arithmetic
        // expressions - not good for BLS engine
        var obj = executeOpenLOGNLExprression(order, MATH_OGNL); // <--
        // problematic,
        // we have
        // to know
        // expresion return type before we invoke it
        // thats something we dont know (and cannot) in BLS
        var value = ((Double) obj);
        assertEquals(orderValue, value, 0.00001);
    }

}
