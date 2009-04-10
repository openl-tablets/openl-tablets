/*
 * Created on Aug 11, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.core.resources.IResource;
import org.openl.eclipse.util.Debug;
import org.openl.util.IConvertor;
import org.openl.util.ISelector;

/**
 * @author sam
 */
public class OpenlLaunchShortcut extends ALaunchShortcut {

    public OpenlLaunchShortcut() {
        Debug.debug("In Openl Launch Shortcut");
    }

    /**
     * Collector: object -> resource -> OpenlLaunchTarget
     */
    @Override
    public IConvertor asLaunchTarget() {
        return new IConvertor() {
            // OpenlCompiler getCompiler(IResource resource)
            // {
            // return new OpenlCompiler();
            // }

            public Object convert(Object o) {
                IResource resource = getResourceAdapter(o);
                if (resource == null) {
                    throw new IllegalArgumentException("Can not find resource for: " + o);
                }

                // if (isOpenlMethod(new
                // EResource(resource).getResourceContent()))
                // {
                // return new OpenlLaunchTarget(resource);
                // }

                return new OpenlModuleLaunchTarget(resource, "main");
            }

        };
    }

    /**
     * Selector: Openl source
     */
    @Override
    public ISelector selectLaunchTargetType() {
        return selectOpenlSource();
    }

}
