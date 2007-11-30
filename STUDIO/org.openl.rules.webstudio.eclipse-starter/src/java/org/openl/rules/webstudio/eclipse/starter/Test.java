/**
 * 
 */
package org.openl.rules.webstudio.eclipse.starter;

/**
 * @author User
 *
 */
public class Test {

    /**
     * @param args
     */
    public static void main(String[] args) {
	Application app = new Application();
	try {
	    app.start(null);
	}
	catch (Exception e){
	    e.printStackTrace();
	}
    }

}
