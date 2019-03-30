package org.openl.ie.constrainer;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
//: Observer.java
/**
 * An abstract class for the observer in the observer-subject (or subscriber-publisher) design pattern.
 */
public abstract class Observer implements EventOfInterest.Constants, java.io.Serializable {
    /**
     * Returns true if the "event" is the one this observer is interested in.
     *
     * @return true if the "event" is the one this observer is interested in.
     *
     * @param event EventOfInterest.
     */
    final public boolean interestedIn(EventOfInterest event) {
        return (subscriberMask() & event.type()) != 0;
    }

    /**
     * Returns the object that owns this observer.
     */
    public abstract Object master();

    /**
     * Returns the subscriber mask for this observer.
     *
     * @return the subscriber mask for this observer.
     */
    public abstract int subscriberMask();

    /**
     * Sets the subscriber mask for this observer.
     */
    public void subscriberMask(int mask, Subject subj) {
    }

    /**
     * Called as a notification about an "event" that occured whith the "subject". This observer should be subscribed to
     * the corresponding event type for this "subject".
     */
    public abstract void update(Subject subject, EventOfInterest event) throws Failure;

}
