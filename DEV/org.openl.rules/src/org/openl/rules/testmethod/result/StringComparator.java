package org.openl.rules.testmethod.result;

/**
 * @author Yury Molchan
 */
class StringComparator extends GenericComparator<String> {

    @Override
    boolean isEmpty(String object) {
        return object.isEmpty();
    }
}
