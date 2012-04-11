package org.openl.rules.common;

import org.openl.rules.common.impl.CommonVersionImpl;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public interface CommonVersion extends Comparable<CommonVersion> {
    public static class CommonVersionConverter implements SingleValueConverter {

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

    int getMajor();

    int getMinor();

    int getRevision();

    String getVersionName();
}
