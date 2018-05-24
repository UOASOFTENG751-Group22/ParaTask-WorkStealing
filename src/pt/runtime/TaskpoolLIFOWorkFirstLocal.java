/*
 * 	3/2/2015
 * 
 * 	Basically a copy of TaskpoolLIFOWorkStealing.
 * 	However, before enqueuing, will check the current registering thread and
 * 	will determine if the queue that the thread will be assigned to is capable of continuing with enqueuing.
 * 	If the queue is too full, then it will automatically stop enqueuing until it hits a certain threshold
 * 	(Similiar to the global counter for each queue instead). Note that it will only affect the queue that is
 * 	currently full.
 */

package pt.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 	
 * 	@author Weng Hao
 *	
 *	Work-First - Local Task Queue Control. Variation of the WorkStealing task scheduler.
 *	While this implementation initially follows the behaviour of the WorkStealing task scheduler,
 *	there is a restriction enforced on each of the local one-off task queues. Each local one-off
 *	task queue can only enqueue a limited number of tasks, this limitation is limited by the
 *	localThreshold variable.
 *	If the number of tasks in a local one-off task queue exceeds this threshold, then the task queue
 *	is no longer able to enqueue any other tasks until the number of tasks no longer exceeds this
 *	threshold. Tasks assigned to these task queues during this period will be processed directly by
 *	the worker thread that is designated to the affected task queue.
 *	The intention for this restriction is to reduce the overall congestion in the task scheduler.
 *	By forcing workers to directly process certain tasks, this will reduce the overall number of tasks
 *	enqueued in the task scheduler in an attempt to improve the overall performance in fine-grained
 *	nested parallelism.
 *
 */

public class TaskpoolLIFOWorkFirstLocal extends TaskpoolLIFOWorkStealing {
	
	/**
	 * 	Threshold value used to control the maximum number of allowable tasks that
	 * 	a local one-off task queue can have at any one time.
	 * 	If the number of tasks that a local one-off task queue exceeds this threshold,
	 * 	then this task queue must then directly process the following task it receives
	 * 	next.
	 * 	If the number of tasks in the local one-off task queue is below this threshold,
	 * 	it is then able to enqueue the tasks as normal.
	 */
	private static int localThreshold = 5;
	
	
	/**
	* When enqueuing a task in the <code>WorkStealing</code> policy, if the task is not able to be executed on any arbitrary thread,
	 * regardless of the type of enqueuing thread it will be enqueued to the <code>privateQueue</code> of the thread in charge of 
	 * executing that task. However, if a task <b>can be executed by arbitrary threads</b>, then if the task is a <code>TaskIDGroup</code> 
	 * it will be enqueued to the <code>globalMultiTask</code> queue, otherwise if the enqueuing thread is a <code>Worker Thread</code> 
	 * and <b>is not</b> a <code>MultiTaskWorker</code> thread, it will enqueue the task to the head of its local queue. For other
	 * cases where the enqueuing thread <b>is</b> a <code>MultiTaskWorker</code> thread or <b>is not</b> a <code>Worker Thread</code>, 
	 * the task will be enqueued to the tail of a random thread's <code>localQueue</code>.
	 * 
	 * @author Mostafa Mehrabi
	 * @since  10/9/2014
	 * */
	@Override
	protected void enqueueReadyTask(TaskID<?> taskID) {
		
		if (taskID.getExecuteOnThread() != ParaTaskHelper.ANY_THREAD_TASK || taskID instanceof TaskIDGroup) {
			//-- this is a multi-task, so place it on that thread's local queue (if it is wrongly stolen, it then gets placed on private queue)
			
			/**
			 * 
			 * @Author : Kingsley
			 * @since : 25/04/2013
			 * The data structure is changed from array to list, therefore the corresponding way to
			 * get the data has to be changed.
			 * 
			 * This is a multi task, so insert it into local Multi Task queue.
			 * 
			 * @since 04/05/2013
			 * There are no local queues for multi task any more.
			 * 
			 * 
			 * */
			//localQueues[taskID.getExecuteOnThread()].add(taskID);
			//localQueues.get(taskID.getExecuteOnThread()).add(taskID);
			
			//localMultiTaskQueues.get(taskID.getExecuteOnThread()).add(taskID);
			if (taskID.getExecuteOnThread() == ParaTaskHelper.ANY_THREAD_TASK) {
				globalMultiTaskqueue.add(taskID);
			} else {
				privateQueues.get(taskID.getExecuteOnThread()).add(taskID);
			}
	
		} else {
			//-- this is a normal task. 
			
			/**
			 * 	Instead of just enqueuing onto the local task queue, the population of the task queue will determine
			 * 	whether enqueuing will occur as usual, or if the task queue is too full, will be forced to
			 * 	manually execute the thread.
			 */
			
			//Get current queuing thread(Could be worker thread or non-worker thread)
			Thread regThread = taskID.getTaskInfo().getRegisteringThread();
			
			/**
			 * 
			 * @Author : Kingsley
			 * @since : 25/04/2013
			 * The data structure is changed from array to list, therefore the corresponding way to
			 * get the data has to be changed.
			 * 
			 * This is a one-off task, so insert it into local One-off Task queue if current
			 * worker thread is dedicated for One-off task.
			 * 
			 * */
			
			if (regThread instanceof WorkerThread) {
				//-- Add task to this thread's worker queue, at the beginning since it is the "hottest" task.
				
				WorkerThread workerThread = (WorkerThread) regThread;				
				
				if (!workerThread.isMultiTaskWorker()) {
					
					/**
					 * 
					 * @Author : Kingsley
					 * @since : 26/04/2013
					 * 
					 * The worker thread here is not multi task worker, should use 
					 * getOneoffTaskThreadID() instead.
					 * 
					 * @since : 02/05/2013
					 * One-off task threads do not need local thread ID. Still use
					 * global id here.
					 * 
					 * */
					//int tid = workerThread.getThreadID();
					//int tid = workerThread.getOneoffTaskThreadID();
					int tid = workerThread.getThreadID();
					
					/**
					 *  If the size of the selected local one-off task queue exceeds the
					 *  localThreshold, then the given task will be processed directly
					 *  instead of being enqueued onto the task queue.
					 */
					if(localOneoffTaskQueues.get(tid).size() >= localThreshold) {
						TaskInfo taskInfo = taskID.getTaskInfo();
						Method m = taskInfo.getMethod();
						try {
							/*
							 * 	Use of a non-generic TaskID to allow the use of a setReturnResult() without
							 * 	using objects for parameterisation.
							 */
							TaskID taskID2 = taskID;
							taskID2.setReturnResult(m.invoke(taskInfo.getInstance(), taskInfo.getParameters()));
							taskID2.setComplete();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						//localQueues[tid].addFirst(taskID);
						//localQueues.get(tid).addFirst(taskID);
						localOneoffTaskQueues.get(tid).addFirst(taskID);
					}
				}else {
					//-- just add it to a random worker thread's queue.. (Add it at the end, since it's not hot in that thread's queue)
					
					/**
					 * 
					 * @Author : Kingsley
					 * @since : 25/04/2013
					 * 
					 * Get a real worker ID array
					 * Get a real worker ID as well
					 * 
					 * @since 18/05/2013
					 * The variable of "numOneoffTaskThreads" is cancelled, whenever want the size
					 * of how many one-off task worker threads, call thread pool directly.
					 * 
					 * When queuing tasks, access thread pool, get the number of alive worker thread first,
					 * only give these threads tasks
					 * 
					 * @since 23/05/2013
					 * Re-structure the code
					 * */
					int oneoffTaskThreadPoolSize = ThreadPool.getOneoffTaskThreadPoolSize();
					Integer[] workIDs = localOneoffTaskQueues.keySet().toArray(new Integer[oneoffTaskThreadPoolSize]);

					//int randThread = (int)(Math.random()*numOneoffTaskThreads);
					int randThread = workIDs[(int)(Math.random()*oneoffTaskThreadPoolSize)];
					
					
					/**
					 *  If the size of the selected local one-off task queue exceeds the
					 *  localThreshold, then the given task will be processed directly
					 *  instead of being enqueued onto the task queue.
					 */
					if(localOneoffTaskQueues.get(randThread).size() >= localThreshold) {
						TaskInfo taskInfo = taskID.getTaskInfo();
						Method m = taskInfo.getMethod();
						try {
							/*
							 * 	Use of a non-generic TaskID to allow the use of a setReturnResult() without
							 * 	using objects for parameterisation.
							 */
							TaskID taskID2 = taskID;
							taskID2.setReturnResult(m.invoke(taskInfo.getInstance(), taskInfo.getParameters()));
							taskID2.setComplete();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						//localQueues[randThread].addLast(taskID);
						//localQueues.get(randThread).addLast(taskID);
						localOneoffTaskQueues.get(randThread).addLast(taskID);
					}
				}
			} else {
				//-- just add it to a random worker thread's queue.. (Add it at the end, since it's not hot in that thread's queue)
				/**
				 * 
				 * @Author Kingsley
				 * @since 25/04/2013
				 * 
				 * Get a real worker ID array
				 * Get a real worker ID as well
				 * 
				 * @since 18/05/2013
				 * The variable of "numOneoffTaskThreads" is cancelled, whenever want the size
				 * of how many one-off task worker threads, call thread pool directly.
				 * 
				 * When queuing tasks, access thread pool, get the number of alive worker thread first,
				 * only give these threads tasks
				 * 
				 * @since 23/05/2013
				 * Re-structure the code
				 * 
				 * */
				int oneoffTaskThreadPoolSize = ThreadPool.getOneoffTaskThreadPoolSize();
				
				Integer[] workIDs = localOneoffTaskQueues.keySet().toArray(new Integer[oneoffTaskThreadPoolSize]);
				
				//int randThread = (int)(Math.random()*numOneoffTaskThreads);
				int randThread = workIDs[(int)(Math.random()*oneoffTaskThreadPoolSize)];
				
				
				/**
				 *  If the size of the selected local one-off task queue exceeds the
				 *  localThreshold, then the given task will be processed directly
				 *  instead of being enqueued onto the task queue.
				 */
				if(localOneoffTaskQueues.get(randThread).size() >= localThreshold) {
					TaskInfo taskInfo = taskID.getTaskInfo();
					Method m = taskInfo.getMethod();
					try {
						/*
						 * 	Use of a non-generic TaskID to allow the use of a setReturnResult() without
						 * 	using objects for parameterisation.
						 */
						TaskID taskID2 = taskID;
						taskID2.setReturnResult(m.invoke(taskInfo.getInstance(), taskInfo.getParameters()));
						taskID2.setComplete();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					//System.out.println("oneoffTaskThreadPoolSize is " + oneoffTaskThreadPoolSize + " randThread is " + randThread);
					
					//localQueues[randThread].addLast(taskID);
					//localQueues.get(randThread).addLast(taskID);
					localOneoffTaskQueues.get(randThread).addLast(taskID);
				}
			}
		}
	}
	
	/**
	 * 	Assigns the threshold value that is used to determine the
	 * 	number of tasks to be enqueued, before Work-First is enforced.
	 * 	In TaskpoolLIFOWorkFirstLocal, the setThreshold() and
	 * 	getThreshold() refers to the number of allowable tasks that
	 * 	a local one-off task queue can enqueue at any one time before
	 * 	it must process tasks directly.
	 * 	The default value for the localThreshold is 5.
	 * 	@param threshold
	 */
	public static void setThreshold(int threshold) {
		localThreshold = threshold;
	}
	
	/**
	 * 	Returns the current threshold value set for the given task
	 * 	scheduler.
	 * 	In TaskpoolLIFOWorkFirstLocal, the getThreshold() refers
	 * 	to the number of allowable tasks that a local one-off task queue
	 * 	can have in their queue at any one time before it must process
	 * 	the next task directly.
	 * 	@return
	 */
	public static int getThreshold() {
		return localThreshold;
	}
}
