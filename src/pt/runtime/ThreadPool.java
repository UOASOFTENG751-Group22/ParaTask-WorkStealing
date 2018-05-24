/**
 * 
 */
package pt.runtime;

import java.lang.reflect.Method;
import java.util.AbstractQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import pt.runtime.ParaTask;
import pt.runtime.TaskID;
import pt.runtime.Taskpool;
import pt.runtime.WorkerThread;
import pt.runtime.ParaTask.ThreadPoolType;


/**
 * @author Kingsley
 * 
 * @since 01/10/2013
 * Remove all the unnecessary code before merging.
 */
public class ThreadPool {

	private static int totalNumberOfThreads = -1;
	
	/* 
	 * Use the field of "oneoffTaskThreadPoolSize" to trace how many one-off task
	 * worker threads should EXACTLY exist in the one-off task thread pool
	 * Use the field of "multiTaskThreadPoolSize" to trace how many one-off task
	 * worker threads should EXACTLY exist in the one-off task thread pool
	 */
	private static int multiTaskThreadPoolSize = 0;
	
	private static int oneOffTaskThreadPoolSize = 0;

	private final static ReentrantLock reentrantLock = new ReentrantLock();
	
	/*
	 * The pools are <code>TreeMaps</code>, the key is the thread id.
	 * The benefit with <code>TreeMaps</code> is that they are already sorted, so 
	 * we can get the worker thread who owns the highest <code>threadID</code>, or get the worker thread just simply using 
	 * its <code>threadID</code> as an index.
	 * <br><br>
	 * One-off task worker thread does not need to be sorted and synchronized.
	 * Using HashMap instead.
	 */
	private static Map<Integer, WorkerThread> oneoffTaskWorkers = new HashMap<Integer, WorkerThread>();

	private static SortedMap<Integer, WorkerThread> multiTaskWorkers = Collections.synchronizedSortedMap(new TreeMap<Integer, WorkerThread>());
	
	
	
	private static int globalID = 0;
	
	
	private static Taskpool taskpool;
		
	protected static void initialize(Taskpool taskpool) {
		ThreadPool.taskpool = taskpool;
		TaskThread.resetTaskThreads();
		initializeWorkerThreads(taskpool);
	}
	
	static void resetThreadPool(){
		globalID = 0;
	}
	
	private static void initializeWorkerThreads(Taskpool taskpool) {
		
		if(multiTaskThreadPoolSize == 0){
			multiTaskThreadPoolSize = Runtime.getRuntime().availableProcessors();
		}
		if(oneOffTaskThreadPoolSize == 0){
			oneOffTaskThreadPoolSize = Runtime.getRuntime().availableProcessors();
		}
		
		totalNumberOfThreads = oneOffTaskThreadPoolSize + multiTaskThreadPoolSize;
		List<AbstractQueue<TaskID<?>>> privateTaskQueues = taskpool.getPrivateTaskQueues();
		Map<Integer, LinkedBlockingDeque<TaskID<?>>> localOneoffTaskQueues = taskpool.getLocalOneoffTaskQueues();
		int multiTaskWorkerID = 0;		
		
		for (int i = 0; i < totalNumberOfThreads; i++, globalID++) {
			if (multiTaskWorkerID < multiTaskThreadPoolSize) {
				WorkerThread workers = new WorkerThread(globalID, multiTaskWorkerID, taskpool, true);
				workers.setPriority(Thread.MAX_PRIORITY);
				workers.setDaemon(true);

				multiTaskWorkers.put(globalID, workers);
				multiTaskWorkerID++;
			
				privateTaskQueues.add(new PriorityBlockingQueue<TaskID<?>>(
						AbstractTaskPool.INITIAL_QUEUE_CAPACITY,
						AbstractTaskPool.FIFO_TaskID_Comparator));
				
				workers.start();
			}else {
			
				WorkerThread workers = new WorkerThread(globalID, globalID, taskpool, false);
				workers.setPriority(Thread.MAX_PRIORITY);
				workers.setDaemon(true);
				oneoffTaskWorkers.put(globalID, workers);
				
				if (localOneoffTaskQueues != null) {
					localOneoffTaskQueues.put(globalID, new LinkedBlockingDeque<TaskID<?>>());
				}
				workers.start();
			}
		}	
	}
	
	protected static int getPoolSize(ThreadPoolType threadPoolType) {
		switch (threadPoolType) {
		case ONEOFF:
			return oneOffTaskThreadPoolSize;
		case MULTI:
			return multiTaskThreadPoolSize;
		default:
			return totalNumberOfThreads;
		}
	}
	
	protected static void setPoolSize(ThreadPoolType threadPoolType, int poolSize) {
		adjustThreadPool(threadPoolType, poolSize);
	}

	
	protected static int getMultiTaskThreadPoolSize() {
		return multiTaskWorkers.size();
	}
	
	protected static int getOneoffTaskThreadPoolSize() {
		return oneoffTaskWorkers.size();
	}

	/*
	 * Adjust the pool size according to the parameter.
	 * Only called after user called setPoolSize(int poolSize);
	 * setMultiTaskThreadPoolSize(int multiTaskThreadPoolSize) or 
	 * setOneoffTaskThreadPoolSize(int oneoffTaskThreadPoolSize)
	 * 
	 * @param ThreadPoolType Indicate different thread pool 
	 * @param newSize
	 * */
	private static void adjustThreadPool(ThreadPoolType type, int newSize) {
		switch (type) {
		case ALL:
			adjustMultiTaskThreadPool(newSize);
			adjustOneOffThreadPool(newSize);
			break;
		case ONEOFF:
			adjustOneOffThreadPool(newSize);
			break;
		case MULTI:
			adjustMultiTaskThreadPool(newSize);
			break;
		default:
			break;
		}
		totalNumberOfThreads = oneOffTaskThreadPoolSize + multiTaskThreadPoolSize;
	}
	
	/* 
	 * Adjust the pool size for multi task thread pool
	 * Work on this part later.
	 * */
	private static void adjustMultiTaskThreadPool(int newSize){
		if(ParaTask.isInitialized())
			dynamicAdjustMultiTaskThreadPool(newSize);
		else
			multiTaskThreadPoolSize = newSize;
	}
	
	private static void dynamicAdjustMultiTaskThreadPool(int newSize){
		//Dynamic resizing of thread-pools, especially the multi-thread pool is not a good idea!
		return;
	}
	
	private static void adjustOneOffThreadPool(int newSize){
		if(ParaTask.isInitialized())
			dynamicAdjustOneOffThreadPool(newSize);
		else 
			oneOffTaskThreadPoolSize = newSize;
	}
	
	private static void dynamicAdjustOneOffThreadPool(int newSize){
		if (oneOffTaskThreadPoolSize == newSize) {
			return;
		}else if (oneOffTaskThreadPoolSize > newSize) {
			int diff = oneOffTaskThreadPoolSize - newSize;

			LottoBox.setLotto(diff);
			
			reentrantLock.lock();
			
			for (Iterator<WorkerThread> iterator = oneoffTaskWorkers.values().iterator(); iterator.hasNext();) {
				WorkerThread workerThread = (WorkerThread) iterator.next();
				workerThread.requireCancel(true);
			}
			
			reentrantLock.unlock();
			
			oneOffTaskThreadPoolSize = oneOffTaskThreadPoolSize - diff;
		}else {
			int diff = newSize - oneOffTaskThreadPoolSize;
			
			for (int i = 0; i < diff; i++, globalID++) {
				WorkerThread workers = new WorkerThread(globalID, globalID, taskpool, false);
				workers.setPriority(Thread.MAX_PRIORITY);
				workers.setDaemon(true);

				oneoffTaskWorkers.put(globalID, workers);
				Map<Integer, LinkedBlockingDeque<TaskID<?>>> localOneoffTaskQueues = taskpool.getLocalOneoffTaskQueues();
				if (null != localOneoffTaskQueues) {
					localOneoffTaskQueues.put(globalID, new LinkedBlockingDeque<TaskID<?>>());
				}
				workers.start();
			}
			
			oneOffTaskThreadPoolSize = oneoffTaskWorkers.size();
		}
	}
	
	
	protected static void lastWords(boolean isMultiTaskWorker, int threadID){
		if (isMultiTaskWorker) {
			
		}else {
			reentrantLock.lock();
			
			oneoffTaskWorkers.remove(threadID);
			
			reentrantLock.unlock();
		}
	}

	/**
	 * 
	 * Returns the approximate number of threads that are actively executing tasks.
	 * */
	public static int getActiveCount(ThreadPoolType type) {
		switch (type) {
		case ONEOFF:
			return oneoffTaskWorkers.size();
		case MULTI:
			return multiTaskWorkers.size();
		default:
			return oneoffTaskWorkers.size()+multiTaskWorkers.size();
		}
	}
 }


