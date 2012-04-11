package com.exigen.ie.constrainer.impl;

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
import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatVar;
import com.exigen.ie.tools.FastVectorDouble;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/*
* Changes:
* 02.20.03 Constrainer.FLOAT_PRECISION is added by SV
*/
/**
 * An implementation of the history for the floating-point domain.
 */
public final class FloatDomainHistory {
  final static int  MIN_IDX =  0;
  final static int  MAX_IDX =  1;
  final static int  LAST_IDX = 2;

  FloatVar          _var;
  double            _min;
  double            _max;

  int               _mask;
  FastVectorDouble  _history;
  int               _currentIndex = -1;

  public FloatDomainHistory (FloatVar var) {
    _var = var;
    _history = new FastVectorDouble (10);
    save ();
  }
  void propagate () throws Failure {
    if ((_var.publisherMask () & _mask) != 0) {
      FloatEventDomain ev = FloatEventDomain.getEvent (this);
      save ();
      _var.notifyObservers (ev);
    } else {
      save ();
    }
  }
  public void saveUndo () {
    if (_mask != 0) {
      save ();
    }
  }
  public String toString () {
    return "History: " + _history + ":" + _currentIndex + "(" +  _min + "-" + _max + ")" + "mask: " + _mask;
  }
  public int currentIndex () { return _currentIndex; }
  int save () {
    int old = _currentIndex;
    _currentIndex = _history.size ();
    _history.add (_min = _var.min ());
    _history.add (_max = _var.max ());
    _mask = 0;
    return old;
  }
  public double min () { return _min; }
  public double max () { return _max; }
  public double oldmin () { return _history.elementAt (_currentIndex + MIN_IDX); }
  public double oldmax () { return _history.elementAt (_currentIndex + MAX_IDX); }
  public void restore (int index) {
    _var.forceMin (_min = _history.elementAt (index + MIN_IDX));
    _var.forceMax (_max = _history.elementAt (index + MAX_IDX));
    _history.cutSize (index + LAST_IDX);
    _currentIndex = index;
    _mask = 0;
  }
  void setMin (double val) {
    if (val > _min + Constrainer.FLOAT_PRECISION) {
      _min = val;
      _mask |= EventOfInterest.MIN;
      if (_var.bound()) {
        _mask |= EventOfInterest.VALUE;
      }
    }
  }
  void setMax (double val) {
    if (val < _max - Constrainer.FLOAT_PRECISION) {
      _max = val;
      _mask |= EventOfInterest.MAX;
      if (_var.bound ()) {
        _mask |= EventOfInterest.VALUE;
      }
    }
  }
  /**
   * An implementation of the event about change in the floating-point domain.
   */
  static final class FloatEventDomain extends FloatEvent {
    static ReusableFactory _factory = new ReusableFactory () {
      protected Reusable createNewElement () {
        return new FloatEventDomain();
      }
    };
    static FloatEventDomain getEvent (FloatDomainHistory history) {
      FloatEventDomain ev = (FloatEventDomain) _factory.getElement ();
      ev.init (history);
      return ev;
    }
    protected double    _min, _max, _oldmin, _oldmax;
    protected int       _type_mask;
    FloatDomainHistory  _history;
    public String name () { return "FloatEventDomain"; }
    public void init (FloatDomainHistory hist) {
      exp(hist._var);
      _min = hist.min ();
      _max = hist.max ();
      _oldmin = hist.oldmin ();
      _oldmax = hist.oldmax ();
      _type_mask = hist._mask;

      _history = hist;
    }
    public double min () { return _min; }
    public int type () { return _type_mask; }
    public double max () { return _max; }
    public double oldmin () { return _oldmin; }
    public double oldmax () { return _oldmax; }
    public double mindiff () { return _min - _oldmin; }
    public double maxdiff () { return _max - _oldmax; }
  }
}