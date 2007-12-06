package org.openl.rules.webstudio.eclipse.starter;

/*import org.eclipse.core.runtime.IPlatformRunnable;*/
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.openl.rules.webtools.StartTomcat;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication/*, IPlatformRunnable*/ {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
	    StartTomcat.main(new String[]{"catalina.base=${eclipse_home}/plugins/org.openl.rules.webstudio_5.0.5/", "catalina.home=${eclipse_home}/plugins/org.openl.lib.apache.tomcat_5.0.5/apache-tomcat-5.5.17"});
	    System.out.println("WebStudio started");
	    return IApplication.EXIT_OK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// nothing to do
	}

	/*
	public Object run(Object arg0) throws Exception {
	    return start(arg0 instanceof IApplicationContext? (IApplicationContext) arg0 : null);
	}*/
}
