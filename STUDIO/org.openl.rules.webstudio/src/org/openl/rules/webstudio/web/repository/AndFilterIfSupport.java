package org.openl.rules.webstudio.web.repository;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.util.ASelector;

class AndFilterIfSupport extends ASelector<AProjectArtefact> implements IFilter<AProjectArtefact> {
    private final IFilter<AProjectArtefact> filter1;
    private final IFilter<AProjectArtefact> filter2;

    AndFilterIfSupport(IFilter<AProjectArtefact> filter1, IFilter<AProjectArtefact> filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return filter1.supports(aClass) || filter2.supports(aClass);
    }

    @Override
    public boolean select(AProjectArtefact obj) {
        if (filter1.supports(obj.getClass())) {
            if (filter2.supports(obj.getClass())) {
                if (filter1.select(obj)) {
                    return filter2.select(obj);
                } else {
                    return false;
                }
            } else {
                return filter1.select(obj);
            }
        } else {
            return filter2.supports(obj.getClass()) && filter2.select(obj);
        }
    }
}
