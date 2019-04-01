package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

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

/**
 * An abstract implementation of the observers used in expressions.
 *
 * Any expression is a subject for other expressions and has an observers for its arguments.
 *
 * ExpressionObserver maintains transformations between publisher event mask for the expression and subscriber event
 * mask for its arguments.
 */
public abstract class ExpressionObserver extends Observer {
    /**
     * An interface for transforming publisher and subscriber masks.
     */
    interface EventMap extends java.io.Serializable {
        /**
         * Transform publisher mask to subscriber mask.
         */
        int publishToSubscribe(int publish_mask);

    }

    /**
     * A generic implementation of the EventMap interface.
     */
    class EventMapImpl implements EventMap {
        private int[] _masks;

        EventMapImpl(int[] masks) {
            _masks = masks;
        }

        @Override
        public int publishToSubscribe(int publish_mask) {
            int result_mask = 0;
            for (int i = 0; i < _masks.length; i += 2) {
                if ((_masks[i + 1] & publish_mask) != 0) {
                    result_mask |= _masks[i];
                }
            }
            return result_mask;
        }

    } // ~ EventMapImpl

    /**
     * Trivial event map.
     */
    static final private int[] trival_event_map = { MIN, MIN, MAX, MAX, VALUE, VALUE, REMOVE, REMOVE };

    // protected Expression _expression;
    private EventMap _event_map;

    /**
     * The mask for event in which the observer is interested in.
     */
    // (eg, from X to X+4)
    private int _subscriber_mask;

    /**
     * Constructor for the ExpressionObserver with a trivial event mask.
     */
    public ExpressionObserver() {
        this(trival_event_map);
    }

    /**
     * Constructor for the ExpressionObserver with a given event map.
     */
    public ExpressionObserver(int[] masks) {
        _event_map = new EventMapImpl(masks);
    }

    // /**
    // * Returns an event map for this observer.
    // */
    // public EventMap eventMap()
    // {
    // return _event_map;
    // }
    //
    /**
     * Subscribes for the events from "exp" with a given "publishMask".
     *
     * This method will be initiated by any subscriber (constraint on this expression or other expression) which wants
     * to be notified about events presented by the "publishMask" (the constraint subscribes to these events).
     */
    public void publish(int publishMask, Expression exp) {
        subscriberMask(_event_map.publishToSubscribe(publishMask), exp);
    }

    @Override
    public int subscriberMask() {
        return _subscriber_mask;
    }

    /**
     * Changes the subscriber mask for this observer.
     */
    @Override
    public void subscriberMask(int mask, Subject subj) {
        if (_subscriber_mask != mask) {
            // Debug.on();Debug.print("" + this + " mask: "+ mask + " exp:" +
            // expression);Debug.off();

            _subscriber_mask = mask;
        }

        subj.reattachObserver(this);
    }

    /**
     * Returns a String representation of this observer.
     *
     * @return a String representation of this observer.
     */
    @Override
    public String toString() {
        return "ExpressionObserver";
    }

    /**
     * Transforms "MAX" event that is send to this observer.
     */
    public void transformMaxEvent(EventOfInterest event) throws Failure {
    }

    /**
     * Transforms "MIN" event that is send to this observer.
     */
    public void transformMinEvent(EventOfInterest event) throws Failure {
    }

    /**
     * Transforms "REMOVE" event that is send to this observer.
     */
    public void transformRemoveEvent(EventOfInterest event) throws Failure {
    }

    /**
     * Transforms "VALUE" event that is send to this observer.
     */
    public void transformValueEvent(EventOfInterest event) throws Failure {
    }

    @Override
    public void update(Subject exp, EventOfInterest event) throws Failure {
        // Debug.on();Debug.print("" + this + " : "+event);Debug.off();
        if (event.isValueEvent()) {
            transformValueEvent(event);
        } else if (event.isMinEvent()) {
            transformMinEvent(event);
        } else if (event.isMaxEvent()) {
            transformMaxEvent(event);
        } else if (event.isRemoveEvent()) {
            transformRemoveEvent(event);
        }
    }

} // ~ ExpressionObserver
