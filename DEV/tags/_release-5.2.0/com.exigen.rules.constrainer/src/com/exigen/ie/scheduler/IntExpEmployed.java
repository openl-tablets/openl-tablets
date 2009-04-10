package com.exigen.ie.scheduler;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.constrainer.impl.IntEvent;
import com.exigen.ie.constrainer.impl.IntExpImpl;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

class IntExpEmployed extends IntExpImpl
{

	private int _value;
	private IntVar _var;
	private Resource _resource;
	private Observer _observer;

	public IntExpEmployed(Resource res)
	{
		super(res.constrainer());
		_resource = res;
		_observer = new EmployedObserver();
		_var = constrainer().addIntVar(0, _resource.duration());
		for (int i = 0; i < _resource.duration(); i++)
			_resource.caps().get(i).attachObserver(_observer);
	}

	public int max()
	{
		return _var.max();
	}
	public int min()
	{
		return _var.min();
	}
	public void setMin(int m) throws Failure
	{
		_var.setMin(m);
	}
	public void setMax(int m) throws Failure
	{
		_var.setMax(m);
	}

	public void attachObserver(Observer observer)
	{
		super.attachObserver(observer);
		_var.attachObserver(observer);
	}

	public void reattachObserver(Observer observer)
	{
		super.reattachObserver(observer);
		_var.reattachObserver(observer);
	}

	public void detachObserver(Observer observer)
	{
		super.detachObserver(observer);
		_var.detachObserver(observer);
	}

	class EmployedObserver extends Observer
	{

		public void update(Subject var, EventOfInterest event) throws Failure
		{
			_value = 0;
			for (int i = 0; i < _resource.duration(); i++)
				if (_resource.caps().get(i).bound())
					_value++;

			_var.setMin(_value);

			IntEvent e = (IntEvent) event;

			IntEventEmployed ev = IntEventEmployed.getEvent(e);

			//notifyObservers(ev);
		}

		public Object master()
		{
			return IntExpEmployed.this;
		}

		public int subscriberMask()
		{
			return EventOfInterest.VALUE;
		}
	}

	//////////////////////////////////////////////////////

	static final class IntEventEmployed extends IntEvent
	{

		static ReusableFactory _factory = new ReusableFactory()
		{
			protected Reusable createNewElement()
			{
				return new IntEventEmployed();
			}

		};

		static IntEventEmployed getEvent(IntEvent event)
		{
			IntEventEmployed ev = (IntEventEmployed) _factory.getElement();
			ev.init(event);
			return ev;
		}

		IntEvent _event;

		int _type = 0;

		void init(IntEvent event)
		{
			_event = event;
			_type |= MIN;
			_type |= MAX;
			_type |= VALUE;
		}

		public int type()
		{
			return _type;
		}

		public int oldmax()
		{
			return _event.oldmax();
		}

		public int oldmin()
		{
			return _event.oldmin();
		}

		public int max()
		{
			return _event.max();
		}

		public int min()
		{
			return _event.min();
		}

		public String name()
		{
			return "IntEventEmployed";
		}

		public int numberOfRemoves()
		{
			return 0;
		}

		public int removed(int i)
		{
			return 0;
		}

	}
}
