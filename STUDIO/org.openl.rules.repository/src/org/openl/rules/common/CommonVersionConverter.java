package org.openl.rules.common;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.openl.rules.common.impl.CommonVersionImpl;

/**
 * Created by ymolchan on 10/6/2014.
 */
class CommonVersionConverter implements SingleValueConverter {

    public boolean canConvert(Class cls) {
        return CommonVersion.class.isAssignableFrom(cls);
    }

    public Object fromString(String souce) {
        return new CommonVersionImpl(souce);
    }

    public String toString(Object obj) {
        // FIXME
        return new CommonVersionImpl((CommonVersion) obj).getVersionName();
    }
}
