package org.openl.rules.testmethod.export;

/**
 * A type that leads to itself through its field, as {@code BaseCalendar} of the JDK does through its fields.
 *
 * @author Yury Molchan
 */
public class Link {

    public String name;
    public Link next;

    public Link(String name, Link next) {
        this.name = name;
        this.next = next;
    }
}
