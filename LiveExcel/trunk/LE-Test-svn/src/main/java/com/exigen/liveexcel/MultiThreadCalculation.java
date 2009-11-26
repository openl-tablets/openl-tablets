/**
 * 
 */
package com.exigen.liveexcel;

import java.io.FileInputStream;
import java.util.ArrayList;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.LiveExcelEvaluator;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack.UDFFinderLE;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;


/**
 * @author zsulkins
 *
 */
public class MultiThreadCalculation {

	LiveExcelEvaluator evaluator = null;
	int threadNumber = 0; 
	static Log log = LogFactory.getLog(MultiThreadCalculation.class);	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// check params
		if (args.length != 4){
			String errMsg = "Usage: MultiThreadName <liveExcelFilePath> <number of threads> <timeout in minutes> <task description file>";
			System.out.println(errMsg);
			log.error(errMsg);
			System.exit(1);
		}
		
		
		
		MultiThreadCalculation test = new MultiThreadCalculation();
		String liveExcelFile = args[0];
		String taskDescriptionFile = args[3];
		int threadCntr = 0;
		int timeout =0;
		List<TasksPortion> tasksPortionList = null;
		
		
		try {
			threadCntr = Integer.valueOf(args[1]).intValue();
		} catch (NumberFormatException e) {
			String errMsg = "second parameter must be int (number of threads to run)";
			System.out.println(errMsg);
			log.error(errMsg);
			System.exit(1);
		}

		try {
			timeout = Integer.valueOf(args[2]).intValue();
		} catch (NumberFormatException e) {
			String errMsg = "third parameter must be int (timeout in min)";
			System.out.println(errMsg);
			log.error(errMsg);
			System.exit(1);
		}
		
//		tasksPortionList = test.mockList();   // for test
		
		tasksPortionList = TasksPortion.getTasks4Book(taskDescriptionFile);
		
		log.info("LiveExcel file: " + liveExcelFile);
		log.info("Number of threads: " + threadCntr);
		log.info("timeout(min): " + timeout);
		log.info("Task Description file: " + taskDescriptionFile);
		
		
		try {
			test.init(liveExcelFile, threadCntr);

			for (TasksPortion tport : tasksPortionList) {
				log.info("Portion name: " + tport.getPortionName());
				test.execute(timeout, test.prepareCallableList(tport));
			}
		} catch (Throwable t) {
			log.fatal("execution failed", t);
		}
	}

	public void init(String liveExcelFilePath, int threadCount)throws Exception {
		
		threadNumber = threadCount;
		LiveExcelWorkbook workbook = LiveExcelWorkbookFactory.create(new FileInputStream(liveExcelFilePath), null /*"SimpleExample"*/);
		new DeclaredFunctionSearcher(workbook).findFunctions();
		evaluator = new LiveExcelEvaluator(workbook, workbook.getEvaluationContext());

		UDFFinderLE udfFinde = LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook);

		Set<String> funcs = udfFinde.getUserDefinedFunctionNames();
		for(String func:funcs){
			log.debug("Find function:"+func);
		}

	}
	
	public Callable<Long> getCalculationTask(final String functionName, final Object[] functionArgs, final Object pattern){
		
		return new Callable<Long>() {
			public Long call() {

				if (!( (pattern instanceof String) || (pattern instanceof Boolean) || (pattern instanceof Double) ))
					throw new IllegalArgumentException("pattern should be String, Boolean or Double but get: " + pattern.getClass().getCanonicalName());
				
				if (log.isDebugEnabled()){
					String params = "";
					for (Object o: functionArgs){
						params = params + "   " + o.toString();
					}
					
					log.debug("Task created: " + "function :" + functionName + "  args: " + params + "  pattern: " + pattern.toString());
				}
				long startTime = System.nanoTime();
				ValueEval res = evaluator.evaluateServiceModelUDF(functionName, functionArgs);
				Object result = transformValueEval(res);
			    long endTime = System.nanoTime();
				if (!result.equals(pattern)){
					throw new RuntimeException("Task result comparison failed: expected: " + pattern.toString() + "   get:" + result.toString());
				}
				log.debug(result);
				
				return new Long(endTime-startTime);
			}		
		};
	}
	
	public void execute(int timeout, List<Callable<Long>> rs) throws Exception{
		  ExecutorService es = Executors.newFixedThreadPool(threadNumber);
		  long starttime = System.nanoTime();
		  try{
			   List<Future<Long>> result = es.invokeAll(rs);
			   es.shutdown();
			   if (!es.awaitTermination(timeout*60, TimeUnit.SECONDS))
				   log.error("failed to calculate in time");
			   double sum = 0;
			   int cntr = 0;
			   for (Future<Long>f : result){
				   if (f.isDone()){
					   cntr++;
					   sum += f.get().longValue();
				   }
			   }
			   log.info("Number of calculations: " + cntr);
			   if (cntr != 0) 
			     log.info("average(milisec.) : " + (sum/cntr)/(1.E6));
			  }finally{
			   if(!es.isShutdown())
			    es.shutdownNow();
			  }
		long endtime = System.nanoTime();
	    log.info("Total (milisec.) : " + (endtime-starttime)/(1.E6));
		
	}
	
	private List<TasksPortion> mockList(){
		Double[] args = {35., 7.}; 
		TaskPrescription tpres = new TaskPrescription("BigLookup",args,"r35c7", 10 );
		List<TaskPrescription> tpresList = new ArrayList<TaskPrescription>();
		tpresList.add(tpres);
		TasksPortion tport = new TasksPortion("mango",tpresList);
		List<TasksPortion> r = new ArrayList<TasksPortion>();
		r.add(tport);
		return r;
		
	}
	
	private Object transformValueEval(ValueEval ev){
		if (ev instanceof StringEval){
			return ((StringEval)ev).getStringValue();
		}
		
		if (ev instanceof NumberEval){
			return ((NumberEval)ev).getNumberValue();
		}
		
		if (ev instanceof BoolEval) {
			return ((BoolEval)ev).getBooleanValue();
		}
		
		if (ev instanceof BlankEval){
			return "";
		}
		
		String errMsg = "expected one of StringEval,NumberEval,BoolEval, BlankEval but get: " + ev.getClass().getCanonicalName();
		log.error(errMsg);
		throw new IllegalArgumentException("expected one of StringEval,NumberEval,BoolEval, BlankEval but get: " + ev.getClass().getCanonicalName());
		
	}
	
	private List<Callable<Long>> prepareCallableList(TasksPortion tport){
		
		List<Callable<Long>> result = new ArrayList<Callable<Long>>();
		boolean alldone = false;
		List<TaskPrescription> tpresList = tport.getTaskList();
		for (int i=0; alldone == false; i++){
			alldone = true; // temporary
			for (TaskPrescription tpres: tpresList){
			   if( i< tpres.getIterationCount()){
				result.add(getCalculationTask(tpres.getFunctionName(),tpres.getArgs(), tpres.getResult()));
				alldone = false;
			   }
			}
		}
		return result;
	}
	
	

}
