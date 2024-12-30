package org.openl.conf;

import org.openl.OpenL;

/**
 * @author Yury Molchan
 */
public interface IOpenLBuilder {

    OpenL build(IUserContext userContext);

}
