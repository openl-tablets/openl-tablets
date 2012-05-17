package org.openl.rules.dt;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.dt.ArrayLoadTest.ITestI;

public class CheckRangeValuesTest {
    @Test
    public void testCheckRangeValues() {
        String[] trueResult = new String[]{ " 1 .. 13","1 ... 13 ","< 4","> 67",">=67",
                " <= 67","67+","[6; 12)","[67 .. 100]","(67; 123]","45 and more",
                "more than 1"," 1.0 .. 13.45","1 ... 13.3 ","< 4.00000001","> 67.09",">=6.7"," <= 67","0.67+",
                "[6.000; 12)","[67 .. 10.0]","(6.7; 123]","4.5 and more","more than 0.1",
                " -1 .. 13K","1M ... -13 ","<-$4K","> $67",">=67M"," <= -67","0.67B+",
                "[6.000; $12)","[67 .. 10.0]","(-123; -6]","4.5K and more","more than -0.1"};
        
        String[] falseResult = new String[]{"6; 12]","67 +"," 1 aa 13",
                                    " <= -67KM","0.67N+","[6.000; $-12)"};
        
        for (int i = 0; i < trueResult.length; i++) {
            Class[] argClasses = {String.class};
            Object[] argObjects = {trueResult[i]};
            
            try {
                assertEquals(new Boolean(true), invokeStaticMethod(DecisionTableHelper.class, "isRangeValue", argClasses, argObjects));
            } catch (Exception e) {
                fail(e.getMessage()); 
            }
        }
        
        for (int i = 0; i < falseResult.length; i++) {
            Class[] argClasses = {String.class};
            Object[] argObjects = {falseResult[i]};
            
            try {
                assertEquals(new Boolean(false), invokeStaticMethod(DecisionTableHelper.class, "isRangeValue", argClasses, argObjects));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private static Boolean invokeStaticMethod(Class targetClass,
            String methodName, Class[] argClasses, Object[] argObjects) throws Exception {

        try {
            Method method = targetClass.getDeclaredMethod(methodName,
                    argClasses);
            method.setAccessible(true);
            
            return (Boolean) method.invoke(null, argObjects);
        } catch (InvocationTargetException e) {
            throw e;
        } catch (NoSuchMethodException e) {
            // Should happen only rarely, because most times the
            // specified method should exist. If it does happen, just let
            // the test fail so the programmer can fix the problem.
            throw e;
        } catch (SecurityException e) {
            // Should happen only rarely, because the setAccessible(true)
            // should be allowed in when running unit tests. If it does
            // happen, just let the test fail so the programmer can fix
            // the problem.
            throw e;
        } catch (IllegalAccessException e) {
            // Should never happen, because setting accessible flag to
            // true. If setting accessible fails, should throw a security
            // exception at that point and never get to the invoke. But
            // just in case, wrap it in a TestFailedException and let a
            // human figure it out.
            throw e;
        } catch (IllegalArgumentException e) {
            // Should happen only rarely, because usually the right
            // number and types of arguments will be passed. If it does
            // happen, just let the test fail so the programmer can fix
            // the problem.
            throw e;
        }
    }
}
