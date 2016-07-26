package org.openl.util.ce.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openl.util.IConvertor;
import org.openl.util.ce.ArrayExecutionException;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.ICallableActivity;
import org.openl.util.ce.IMTConvertorFactory;
import org.openl.util.ce.IScheduler;
import org.openl.util.ce.conf.ServiceMTConfiguration;

public class ServiceMT16 extends ServiceMT {

	public ServiceMT16(ServiceMTConfiguration config) {
		super(config);
		serviceImpl = Executors.newCachedThreadPool();
	}

	ExecutorService serviceImpl;

	@Override
	public <T> long executeAll(Callable<T>[] calls, T[] result,
			long durationEstimate) throws ArrayExecutionException {

		long start = System.nanoTime();

		List<Future<T>> res;

		SortedMap<Integer, Throwable> errors = null;
		try {
			res = serviceImpl.invokeAll(Arrays.asList(calls));
		} catch (InterruptedException e) {
			throw new ArrayExecutionException("Interrupted Exception",
					new TreeMap<Integer, Throwable>());
		}

		for (int i = 0; i < result.length; i++) {
			try {
				result[i] = res.get(i).get();
			} catch (Throwable t) {
				if (errors == null)
					errors = new TreeMap<Integer, Throwable>();
				errors.put(i, t);
				if (errors.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit Reached: ",
							errors);
			}
		}

		if (errors != null)
			throw new ArrayExecutionException("Errors:", errors);

		return System.nanoTime() - start;
	}

	@Override
	public <A, T> long executeArray(IConvertor<A, T> conv, A[] inputArray,
			T[] result, long durationEstimate) throws ArrayExecutionException {

		long start = System.nanoTime();

		int len = inputArray.length;

		int splitSize = calcSplitSize(len, durationEstimate);

		int flen;
		List<Future<?>> fres = new ArrayList<Future<?>>();

		for (int i = 0; i < len; i += splitSize) {
			Runnable task = makeRunnable1(conv, inputArray, i,
					Math.min(i + splitSize, len), result);
			Future<?> f = serviceImpl.submit(task);
			fres.add(f);
		}

		flen = fres.size();
		SortedMap<Integer, Throwable> errors = null;
		for (int i = 0; i < flen; i++) {

			try {
				fres.get(i).get();
			} catch (Throwable t) {
				if (errors == null)
					errors = new TreeMap<Integer, Throwable>();
				errors.put(i, t);
				if (errors.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit Reached: ",
							errors);
			}
		}

		if (errors != null)
			throw new ArrayExecutionException("Errors:", errors);

		return System.nanoTime() - start;
	}

	static <A, T> Runnable makeRunnable1(final IConvertor<A, T> conv,
			final A[] inputArray, final int from, final int to, final T[] result) {

		return new Runnable() {

			@Override
			public void run() {
				try {
					activeThreadCounter.incrementAndGet();
					for (int i = from; i < to; i++) {
						T t = conv.convert(inputArray[i]);
						result[i] = t;
					}
				} finally {
					activeThreadCounter.decrementAndGet();
				}
			}
		};
	}

	public <T> long execute(ICallableActivity<T>[] all, boolean isAsymmerical)
			throws ArrayExecutionException {
		long start = System.nanoTime();

		int len = all.length;

		long totalEstimate = calcTotalEstimate(all);

		if (totalEstimate < config.getMinSequenceLengthNs() * 2) {
			return executeAllSequential(all, null, 0, len);
		}

		if (isAsymmerical)
			Arrays.sort(all);

		int n = Math.min(
				(int) (totalEstimate / config.getMinSequenceLengthNs()),
				config.getTotalParallelLevel());

		List<ICallableActivity<T>>[] split = EvenSplitter.split(all, n);

		int flen = n;
		List<Future<?>> fres = new ArrayList<Future<?>>();

		for (int i = 0; i < flen; i++) {
			List<ICallableActivity<T>> acts = split[i];
			if (acts == null || acts.isEmpty())
				continue;

			Runnable task = makeRunnableActivities(acts);
			Future<?> f = serviceImpl.submit(task);
			fres.add(f);
		}

		flen = fres.size();
		SortedMap<Integer, Throwable> errors = null;
		for (int i = 0; i < flen; i++) {

			try {
				fres.get(i).get();
			} catch (Throwable t) {
				if (errors == null)
					errors = new TreeMap<Integer, Throwable>();
				errors.put(i, t);
				if (errors.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit Reached: ",
							errors);
			}
		}

		if (errors != null)
			throw new ArrayExecutionException("Errors:", errors);

		return System.nanoTime() - start;
	}

	protected <T> Runnable makeRunnableActivities(
			final List<ICallableActivity<T>> acts) {
		return new Runnable() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void run() {
				try {
					activeThreadCounter.incrementAndGet();
					int len = acts.size();
					ICallableActivity[] aa = acts
							.toArray(new ICallableActivity[len]);
					executeAllSequential(aa, null, 0, len);
				} finally {
					activeThreadCounter.decrementAndGet();
				}
			}
		};
	}

	@Override
	public <T> long executeIndexed(IMTConvertorFactory<T> factory, T[] result)
			throws ArrayExecutionException {
		long start = System.nanoTime();

		int len = result.length;

		int splitSize = calcSplitSize(len, factory.estimateDuration(0));

		// int splitSize = 4;

		// int flen = len / splitSize + 1;
		List<Future<?>> fres = new ArrayList<Future<?>>();

		for (int i = 0; i < len; i += splitSize) {
			Runnable task = makeRunnable2(factory.makeConvertorInstance(), i,
					Math.min(i + splitSize, len), result);
			Future<?> f = serviceImpl.submit(task);
			fres.add(f);
		}

		int flen = fres.size();
		SortedMap<Integer, Throwable> errors = null;
		for (int i = 0; i < flen; i++) {

			try {
				fres.get(i).get();
			} catch (Throwable t) {
				if (errors == null)
					errors = new TreeMap<Integer, Throwable>();
				errors.put(i, t);
				if (errors.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit Reached: ",
							errors);
			}
		}

		if (errors != null)
			throw new ArrayExecutionException("Errors:", errors);

		return System.nanoTime() - start;
	}

	@Override
	public <T> long executeIndexed(IConvertor<Integer, T> conv, T[] result,
			long durationEstimate) throws ArrayExecutionException {
		long start = System.nanoTime();

		int len = result.length;

		int splitSize = calcSplitSize(len, durationEstimate);

		// int flen = len / splitSize + 1;
		List<Future<?>> fres = new ArrayList<Future<?>>();

		for (int i = 0; i < len; i += splitSize) {
			Runnable task = makeRunnable2(conv, i,
					Math.min(i + splitSize, len), result);
			Future<?> f = serviceImpl.submit(task);
			fres.add(f);
		}

		int flen = fres.size();
		SortedMap<Integer, Throwable> errors = null;
		for (int i = 0; i < flen; i++) {

			try {
				fres.get(i).get();
			} catch (Throwable t) {
				if (errors == null)
					errors = new TreeMap<Integer, Throwable>();
				errors.put(i, t);
				if (errors.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit Reached: ",
							errors);
			}
		}

		if (errors != null)
			throw new ArrayExecutionException("Errors:", errors);

        return System.nanoTime() - start;
	}


	static <T> Runnable makeRunnable2(final IConvertor<Integer, T> conv,
			final int from, final int to, final T[] result) {

		return new Runnable() {

			@Override
			public void run() {
				try {
					activeThreadCounter.incrementAndGet();
					for (int i = from; i < to; i++) {
						T t = conv.convert(i);
						result[i] = t;
					}
				} finally {
					activeThreadCounter.decrementAndGet();
				}
			}
		};
	}

	@Override
	public long executeAll(Runnable[] tasks) throws ArrayExecutionException {
		long start = System.nanoTime();

		Future<?>[] ff = new Future<?>[tasks.length];

		for (int i = 0; i < ff.length; i++) {
			ff[i] = serviceImpl.submit(tasks[i]);
		}

		SortedMap<Integer, Throwable> errors = null;
		for (int i = 0; i < ff.length; i++) {
			try {
				ff[i].get();
			} catch (Throwable t) {
				if (errors == null)
					errors = new TreeMap<Integer, Throwable>();
				errors.put(i, t);
				if (errors.size() >= config.getErrorLimit())
					throw new ArrayExecutionException("Error Limit Reached: ",
							errors);
			}
		}

		if (errors != null)
			throw new ArrayExecutionException("Caught " + errors.size()
					+ " error(s)", errors);

        return System.nanoTime() - start;
	}

	
	@Override
	protected void finalize() throws Throwable {
		try {
			serviceImpl.shutdown();
		} catch (Exception ignored) {
		} finally {
			super.finalize();
		}
	}

	@Override
	public <T> long executeIndexedPrimitive(IConvertor<Integer, Object> conv,
			Object results, long durationEstimate)
			throws ArrayExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown() {
		serviceImpl.shutdown();

	}

	@Override
	public IScheduler getScheduler(long singleCellLength) {
		return new Scheduler(config, singleCellLength);
	}

}
