package org.openl.util.ce.impl;

import java.lang.reflect.Array;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.openl.util.IConvertor;
import org.openl.util.ce.ArrayExecutionException;
import org.openl.util.ce.IServiceMT;
import org.openl.util.ce.conf.IServiceMTConfiguration;

public abstract class ServiceBase implements IServiceMT {

	protected IServiceMTConfiguration config;

	public ServiceBase(IServiceMTConfiguration config) {
		super();
		this.config = config;
	}

	public IServiceMTConfiguration getConfig() {
		return config;
	}
	

	
	public <T> long executeAllSequential(Callable<T>[] calls, T[] result, int from, int to)
			throws ArrayExecutionException {


		SortedMap<Integer, Throwable> errs = null;
		long startTime = System.nanoTime();
		
		for (int i = from; i < to; i++) {
			try {
				
				T res = calls[i].call();
				if (result != null)
					result[i] = res;
			} catch (Throwable t) {
				
				if (errs == null)
					errs = new TreeMap<Integer, Throwable>();
				errs.put(i, t);
				if (errs.size() > config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit exceeded " + config.getErrorLimit() , errs);
			}
		}

		if (errs != null)
			throw new ArrayExecutionException("Caught " + errs.size() + " error(s)", errs);
		
		return System.nanoTime() - startTime;
	}
	
	public <A, T> long executeArraySequential(IConvertor<A, T> conv, A[] inputArray,
			T[] result, int from, int to) throws ArrayExecutionException {

		SortedMap<Integer, Throwable> errs = null;
		long startTime = System.nanoTime();
		
		for (int i = from; i < to; i++) {
			try {
				result[i] = conv.convert(inputArray[i]);
			} catch (Throwable t) {
				
				if (errs == null)
					errs = new TreeMap<Integer, Throwable>();
				errs.put(i, t);
				if (errs.size() > config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit exceeded " + config.getErrorLimit() , errs);
			}
		}
		
		if (errs != null)
			throw new ArrayExecutionException("Caught " + errs.size() + " error(s)", errs);
		
		return System.nanoTime() - startTime;
	}
	
	public <T> long executeIndexedSequential(IConvertor<Integer, T> conv, T[] result, int from, int to) throws ArrayExecutionException {
		SortedMap<Integer, Throwable> errs = null;
		long startTime = System.nanoTime();
		
		for (int i = from; i < to; i++) {
			try {
				result[i] = conv.convert(i);
			} catch (Throwable t) {
				
				if (errs == null)
					errs = new TreeMap<Integer, Throwable>();
				errs.put(i, t);
				if (errs.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit exceeded " + config.getErrorLimit() , errs);
			}
		}
		
		if (errs != null)
			throw new ArrayExecutionException("Caught " + errs.size() + " error(s)", errs);
		
		
		return System.nanoTime() - startTime;
	}

	public Long executeIndexedSequentialPrimitive(
			IConvertor<Integer, Object> conv, Object result, int from, int to) {
		SortedMap<Integer, Throwable> errs = null;
		long startTime = System.nanoTime();
		
		for (int i = from; i < to; i++) {
			try {
				Object res  = conv.convert(i);
				Array.set(result, i, res);
			} catch (Throwable t) {
				
				if (errs == null)
					errs = new TreeMap<Integer, Throwable>();
				errs.put(i, t);
				if (errs.size() > config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit exceeded " + config.getErrorLimit() , errs);
			}
		}

		if (errs != null)
			throw new ArrayExecutionException("Caught " + errs.size() + " error(s)", errs);
		
		
		return System.nanoTime() - startTime;
	}
	
	public long executeAllSequential(Runnable[] tasks, int from, int to) {
		SortedMap<Integer, Throwable> errs = null;
		long startTime = System.nanoTime();
		
		for (int i = from; i < to; i++) {
			try {
				tasks[i].run();
			} catch (Throwable t) {
				
				if (errs == null)
					errs = new TreeMap<Integer, Throwable>();
				errs.put(i, t);
				if (errs.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit exceeded " + config.getErrorLimit() , errs);
			}
		}
		
		if (errs != null)
			throw new ArrayExecutionException("Caught " + errs.size() + " error(s)", errs);
		
		
		return System.nanoTime() - startTime;
	}
	
	
}
