package org.openl.util.ce.impl;

import java.lang.reflect.Array;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.openl.util.IConvertor;
import org.openl.util.ce.ArrayExecutionException;
import org.openl.util.ce.IMTConvertorFactory;
import org.openl.util.ce.IServiceMTConfiguration;

public class ServiceMT17  extends ServiceMT {

	public ServiceMT17(IServiceMTConfiguration config) {
		super(config);
		fjpool = new ForkJoinPool(config.getParallelLevel());

	}
	
	ForkJoinPool fjpool;


	@Override
	public <T> long executeIndexed(IConvertor<Integer, T> conv, T[] result,
			long durationEstimate) throws ArrayExecutionException {
		
		long start =  System.nanoTime();
		
		
		int splitSize = calcSplitSize(result.length, durationEstimate);
		
		Task<T> t = new Task<T>(conv, result, 0, result.length, splitSize);
		
		fjpool.invoke(t);
		
		return System.nanoTime() - start;
	}

	
	
	
	@Override
	public <T> long executeAll(Callable<T>[] calls, T[] result,
			long durationEstimate) throws ArrayExecutionException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <A, T> long executeArray(IConvertor<A, T> conv, A[] inputArray,
			T[] result, long durationEstimate) throws ArrayExecutionException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getBusyRatio() {
		return 0;
	}

	@Override
	protected void finalize() throws Throwable {
		
		fjpool.shutdown();
	}
	
		
	
	class Task<T> extends RecursiveTask<Long>
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		
		IMTConvertorFactory<T> factory;
		IConvertor<Integer, T> conv; 
		T[] result; 
		int from; 
		int to;
		int splitSize;

		

		public Task(IConvertor<Integer, T> conv, T[] result, int from, int to,
				int splitSize) {
			super();
			this.conv = conv;
			this.result = result;
			this.from = from;
			this.to = to;
			this.splitSize = splitSize;
		}

public Task(IMTConvertorFactory<T> factory, T[] result, int from,
				int to, int splitSize) {

	super();
	this.factory = factory;
	this.result = result;
	this.from = from;
	this.to = to;
	this.splitSize = splitSize;

}

/*
		 *     if (hi - lo < THRESHOLD)
			 *       sequentiallySort(array, lo, hi);
			 *     else {
			 *       int mid = (lo + hi) >>> 1;
			 *       invokeAll(new SortTask(array, lo, mid),
			 *                 new SortTask(array, mid, hi));
			 *       merge(array, lo, hi);
			 *     }
	*/	
		

		@Override
		protected Long compute() {
			
			int size = to - from;
			if (size <= splitSize)
			{	
				return executeIndexedSequential(factory == null? conv : factory.makeConvertorInstance() , result, from, to);
			}	
				
			int mid = (from + to)/2;
			
			if (factory == null)
			{	
				Task<T> t1 = new Task<T>(conv, result, from, mid, splitSize);
				Task<T> t2 = new Task<T>(conv, result, mid, to, splitSize);
				invokeAll(t1, t2);
			}
			else
			{
				Task<T> t1 = new Task<T>(factory, result, from, mid, splitSize);
				Task<T> t2 = new Task<T>(factory, result, mid, to, splitSize);
				invokeAll(t1, t2);
				
			}	
				
			
			return 0L;
		}

		
	}


	
	class TaskPrimitive extends RecursiveTask<Long>
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		IConvertor<Integer, Object> conv; 
		Object result; 
		int from; 
		int to;
		int splitSize;

		

		public TaskPrimitive(IConvertor<Integer, Object> conv, Object result, int from, int to,
				int splitSize) {
			super();
			this.conv = conv;
			this.result = result;
			this.from = from;
			this.to = to;
			this.splitSize = splitSize;
		}

/*
		 *     if (hi - lo < THRESHOLD)
			 *       sequentiallySort(array, lo, hi);
			 *     else {
			 *       int mid = (lo + hi) >>> 1;
			 *       invokeAll(new SortTask(array, lo, mid),
			 *                 new SortTask(array, mid, hi));
			 *       merge(array, lo, hi);
			 *     }
	*/	
		

		@Override
		protected Long compute() {
			
			int size = to - from;
			if (size <= splitSize)
				return executeIndexedSequentialPrimitive(conv, result, from, to);
				
			int mid = (from + to)/2;
			
			TaskPrimitive t1 = new TaskPrimitive(conv, result, from, mid, splitSize);
			TaskPrimitive t2 = new TaskPrimitive(conv, result, mid, to, splitSize);
			
			invokeAll(t1, t2);
			return 0L;
		}


		
	}
	

	@Override
	public <T> long executeIndexedPrimitive(IConvertor<Integer, Object> conv,
			Object results, long durationEstimate) {
		long start =  System.nanoTime();
		
		int len = Array.getLength(results);
		
		int splitSize = calcSplitSize(len, durationEstimate);
		
		TaskPrimitive t = new TaskPrimitive(conv, results, 0, len, splitSize);
		
		fjpool.invoke(t);
		
		return System.nanoTime() - start;
	}




	@Override
	public <T> long executeIndexed(IMTConvertorFactory<T> factory, T[] result)
			throws ArrayExecutionException {
		long start =  System.nanoTime();
		
		
		int splitSize = calcSplitSize(result.length, factory.estimateDuration(0));
		
		Task<T> t = new Task<T>(factory, result, 0, result.length, splitSize);
		
		fjpool.invoke(t);
		
		return System.nanoTime() - start;
	}




	@Override
	public void shutdown() {
		fjpool.shutdown();
		
	}

}
