package org.openl.rules.ruleservice.publish.lazy;

import org.openl.types.IOpenMember;

public class LazyMemberEvent implements Event {

    private LazyMember<? extends IOpenMember> lazyMember;

    public LazyMemberEvent(LazyMember<? extends IOpenMember> lazyMember) {
        if (lazyMember == null) {
            throw new NullPointerException();
        }
        this.lazyMember = lazyMember;
    }

    @Override
    public void onEvent() {
        lazyMember.clearCachedMember();
    }
}
