/**
 *
 */
package org.openl.rules.ruleservice.loader;

import java.util.EventListener;

/**
 * @author Sergey Zyrianov
 *
 */
public interface LoadingListener extends EventListener {

    void onAfterLoading(LoadingEventObject loadedDeployment);

    void onBeforeLoading(LoadingEventObject loadingDeployment);

}
