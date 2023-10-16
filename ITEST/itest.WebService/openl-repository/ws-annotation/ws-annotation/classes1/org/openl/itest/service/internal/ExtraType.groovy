package org.openl.itest.service.internal

import com.fasterxml.jackson.annotation.JsonRootName
import org.openl.generated.beans.RuleType;

@JsonRootName("RootType")
class ExtraType extends RuleType {

    public String extra;
}
