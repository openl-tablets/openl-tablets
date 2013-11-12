/**
 * Created Feb 19, 2007
 */
package org.openl.main;

import java.util.ArrayList;
import java.util.List;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObject;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */

public abstract class OpenLWrapperController {
    static public final int DEBUG_MODE = 1;
    static public final int REGULAR_MODE = 0;

    /**
     * Each call to the reload() method creates new version of IOpenClass loaded
     * from some source; it does not matter if the actual source have been
     * updated or not
     *
     * @param sameLocation indicates whether to use the location where the
     *            previous version was loaded from,or to repeat the search
     *            procedure; In case if the previous search did not produce a
     *            valid location the search will be conducted anyway.
     *
     */

    static public final String AUTORELOAD_OFF = "org.openl.wrapper.autoreload.mode.off",
            AUTORELOAD_ON = "org.openl.wrapper.autoreload.mode.on";

    protected String autoReloadMode = AUTORELOAD_OFF;

    List<CompiledOpenClass[]> versions = new ArrayList<CompiledOpenClass[]>();

    protected OpenL openl;

    /**
     *
     * @return CompiledOpenClass - it is a safe operation, it does not throw
     *         exceptions in case when there were compilation errors
     */

    public CompiledOpenClass getCompiledOpenClass() {
        return getCompiledOpenClass(REGULAR_MODE);
    }

    public CompiledOpenClass getCompiledOpenClass(int mode) {
        return getCompiledOpenClass(mode, 0);
    }

    /**
     * @param mode
     * @param version - 0 - the latest version, -1 - previous etc.; please keep
     *            in mind that only for the latest version the method gurantees
     *            to return both modes; for previous versions it depends on
     *            whether there were instances of the particular mode created in
     *            memmory
     * @return
     */
    public synchronized CompiledOpenClass getCompiledOpenClass(int mode, int version) {
        CompiledOpenClass[] ch = getVersion(version);

        if (version == 0 && ch[mode] == null) {
            ch[mode] = loadCompiledOpenClass(mode);
        }

        return ch[mode];
    }

    public IRuntimeEnv getEnv() {
        return getOpenL().getVm().getRuntimeEnv();
    }

    public DynamicObject getInstance() {
        return getInstance(REGULAR_MODE);
    }

    /**
     * @param regular_mode
     * @return
     */

    public DynamicObject getInstance(int mode) {
        return (DynamicObject) getOpenClass(mode).newInstance(getEnv());
    }

    public IOpenClass getOpenClass() {
        return getOpenClass(REGULAR_MODE);
    }

    /**
     * This method will throw a SyntaxError if there were any compiler errors
     *
     * @param mode
     * @return
     */

    public IOpenClass getOpenClass(int mode) {
        CompiledOpenClass co = getCompiledOpenClass(mode);
        return co.getOpenClass();
    }

    public synchronized OpenL getOpenL() {
        if (openl == null) {
            openl = OpenL.getInstance(getOpenLName());
        }
        return openl;
    }

    public abstract String getOpenLName();

    public abstract String getSource();

    /**
     *
     * @return the search path separated by ';'
     */
    public abstract String getUserHomeSearchPath();

    private synchronized CompiledOpenClass[] getVersion(int version) {
        if (version < 0) {
            return versions.get(versions.size() - 1 + version);
        }
        // version == 0 - latest

        if (versions.size() == 0) {
            versions.add(new CompiledOpenClass[2]);
        }
        return versions.get(versions.size() - 1);
    }

    /**
     * @param mode
     * @return
     */
    private CompiledOpenClass loadCompiledOpenClass(int mode) {
        return null;
    }

    public void reload() {
        reload(true);
    }

    public abstract void reload(boolean sameLocation);

    /**
     * If mode is AUTORELOAD_ON, each call to getOpenClass() will check if
     * source have been changed, and if yes - the new version will be reloaded;
     * it does not matter if new version has errors or not. Also, all the old
     * instances that were created before will maintain reference to the older
     * version of the IOpenClass. It is application responcibility to make sure
     * that new version of the IOpenClass is used.
     *
     *
     * @param mode
     */
    public void setAutoReload(String mode) {
        autoReloadMode = mode;
    }

}
