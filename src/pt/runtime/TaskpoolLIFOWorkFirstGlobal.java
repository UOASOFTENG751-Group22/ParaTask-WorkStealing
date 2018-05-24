package pt.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * 	@author Weng Hao
 * 
 * 	Work-First - Global Task Population Control. Variation of the WorkStealing task scheduler.
 * 	While this implementation initially follows the behaviour of the WorkStealing task scheduler,
 * 	its enqueuing system is restricted by the number of tasks that are enqueued in the system at
 * 	any given time. The intention of this task enqueuing restriction is to allow the task scheduler to
 * 	reduce the overall congestion, by forcing workers to temporarily process tasks directly without
 * 	enqueuing these tasks in the task scheduler.
 * 	The Global Task Population Control overseas the number of tasks available in the system by
 * 	using an AtomicInteger as a counter to determine the number of active tasks available in the 
 * 	task scheduler. If this counter exceeds the workFirstUpperThreshold, then the task scheduler
 * 	will halt all tasks from being enqueued and will force workers to process tasks directly instead.
 * 	The workFirstLowerThreshold is used to indicate to the task scheduler that it can resume
 * 	enqueuing again. 
 * 
 * 	The AtomicInteger, workFirstCounter, will be updated whenever a task has been enqueued (incremented)
 * 	or executed/dequeued (decremented).
 *
 */

public class TaskpoolLIFOWorkFirstGlobal extends TaskpoolLIFOWorkStealing {
	

	/*
	 * 	Used for the Work-First Global Task Population Control.
	 * 	The Counter is used to check if the threshold for the number of allowable enqueued tasks have been met.
	 */
	protected AtomicInteger workFirstCounter = new AtomicInteger(0);

	private static int upperBoundThreshold = 70;
	private static int lowerBoundThreshold = 10;
	private boolean isWorkFirstInPlace = false;
	

	
	
	/**
	 * 	@Override
	 * 	Creates a TaskID for the specified task (whose details are contained in the TaskInfo). The enqueuing process is 
	 * 	dependent on the conditions of the Work-First thresholds. If the number of tasks queued exceeds the 
	 * 	workFirstUpperThreshold, then tasks are no longer queued but executed sequentially - Otherwise tasks will be 
	 * 	queued.
	 * 	It then returns the TaskID after the task has been queued or executed via Work-First conditions. 
	 */
	public TaskID enqueue(TaskInfo taskinfo) {
		
		/**
		 * 	The following boolean statements are used to determine when Work-First is to be enforced.
		 * 	Work-First is enforced when the total number of tasks currently enqueued in the system exceeds
		 * 	the upper bound threshold.
		 * 	If Work-First is enforced, then the tasks created during this period will no longer be
		 * 	enqueued, but will be directly processed by a worker.
		 * 	When the total population of enqueued tasks is below the workFirstLowerThreshold, then the
		 * 	task scheduler will resume enqueuing tasks again.
		 */
		if(workFirstCounter.get() >= upperBoundThreshold)
			isWorkFirstInPlace = true;
		else if(workFirstCounter.get() <= lowerBoundThreshold)
			isWorkFirstInPlace = false;
		
		TaskID taskID = new TaskID(taskinfo);
		
		/**
		 * 	When the Work-First condition is used, it will consider the work-first threshold and will stop enqueuing 
		 * 	when the threshold has been reached.
		 * 	Instead of enqueuing, tasks will be processed directly by the worker thread instead.
		 */
		if(isWorkFirstInPlace) {
			/*
			 * 	Directly extracts the method of the task to operate on the task directly.
			 * 	Also while directly invoking the method of the task, the return result has also been set.
			 */
			try {
				
				Method m = taskinfo.getMethod();
				taskID.setReturnResult(m.invoke(taskinfo.getInstance(), taskinfo.getParameters()));
				
				/*
				 * 	Once successfully invoked, clean up the rest of the TaskID info.
				 */
				taskID.setComplete();
				
				
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
			/**
			 * 	Tasks will be enqueued as normal if the conditions for Work-First have not been met.
			 */
			ArrayList<TaskID<?>> allDependences = null;
			if (taskinfo.getDependences() != null)
				allDependences = ParaTask.allTasksInList(taskinfo.getDependences());
			
			//-- determine if this task is being enqueued from within another task.. if so, set the enclosing task (needed to 
			//--		propogate exceptions to outer tasks (in case they have a suitable handler))
			Thread rt = taskinfo.setRegisteringThread();
			
			if (rt instanceof TaskThread)
				taskID.setEnclosingTask(((TaskThread)rt).currentExecutingTask());
			
			if (taskinfo.hasAnySlots())
				taskinfo.setTaskIDForSlotsAndHandlers(taskID);
			
			if (taskID.isPipeline()) {
				//-- pipeline threads don't need to wait for dependencies
				startPipelineTask(taskID);
			} else if (allDependences == null) {
				if (taskID.isInteractive())
					startInteractiveTask(taskID);
				else
					enqueueReadyTask(taskID);
			} else {
				enqueueWaitingTask(taskID, allDependences);
			}
		}
		
		return taskID;
	}
	
	
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
			if (taskID.getExecuteOnThread() == ParaTaskHelper.ANY_THREAD_TASK) {
				globalMultiTaskqueue.add(taskID);
			} else {
				privateQueues.get(taskID.getExecuteOnThread()).add(taskID);
			}
	
		} else {
			Thread regThread = taskID.getTaskInfo().getRegisteringThread();
			if (regThread instanceof WorkerThread) {
				WorkerThread workerThread = (WorkerThread) regThread;
				if (!workerThread.isMultiTaskWorker()) {
					int tid = workerThread.getThreadID();
					workFirstCounter.incrementAndGet();
					localOneoffTaskQueues.get(tid).addFirst(taskID);
				}else {
					int oneoffTaskThreadPoolSize = ThreadPool.getOneoffTaskThreadPoolSize();
					Integer[] workIDs = localOneoffTaskQueues.keySet().toArray(new Integer[oneoffTaskThreadPoolSize]);
					int randThread = workIDs[(int)(Math.random()*oneoffTaskThreadPoolSize)];
					workFirstCounter.incrementAndGet();
					localOneoffTaskQueues.get(randThread).addLast(taskID);
				}
			} else {
				int oneoffTaskThreadPoolSize = ThreadPool.getOneoffTaskThreadPoolSize();
				Integer[] workIDs = localOneoffTaskQueues.keySet().toArray(new Integer[oneoffTaskThreadPoolSize]);
				int randThread = workIDs[(int)(Math.random()*oneoffTaskThreadPoolSize)];
				workFirstCounter.incrementAndGet();
				localOneoffTaskQueues.get(randThread).addLast(taskID);
			}
		}
	}
	
	
	/**
	 * Only worker threads can call this method.
	 * This method first checks if the worker thread is a multi-task worker thread. In that case, it will check the thread's
	 * <code>privateQueue</code>, and if a task is found, and the attempt for executing it is successful, the task will be 
	 * passed to the thread to execute.
	 * <br><br>
	 * However, if the thread's <code>privateQueue</code> is empty, the method searches through the <code>globalMultiTask</code>
	 * queue, expands each multi-task into its sub-tasks and enqueues them as <code>ready-to-execute</code> tasks. However, the
	 * thread will temporarily return without having anything to execute this time.
	 * <br><br>
	 * If the worker thread is not a multi-task worker thread, it is first attempted to poll a task from the head of that
	 * thread's <code>localOneOffTask</code> queue. If a task is found and the preliminary attempt for executing it is 
	 * successful, that task will be passed to the thread to execute.
	 * <br><br>
	 * However, if there are no tasks in the thread's <code>localOneOffTask</code> queue, the thread will try to steal a task 
	 * from the tail of another thread's <code>localOneOffTask</code> queue. Preferably, if there is a thread from which a
	 * task has been stolen already (AKA <b><i>victim thread</i></b>), we would like to steal from the same victim's 
	 * <code>localOneOffTask</code> queue. 
	 * <br><br>
	 * But if there isn't any previous victims for task stealing, starting from a random thread's <code>localOneOffTask</code>
	 * queue, we proceed through every thread's <code>localOneOffTask</code> queue (except for the current thread's own queue)
	 * and we look for a task to steal from the tail of that local queue. Once a task is found, and the preliminary attempt 
	 * for executing it is successful, that task will be passed to the thread, and that <code>localOneOffTask</code> queue's 
	 * corresponding thread will be remembered as the <b><i>victim thread</i></b>.
	 * <br><br>
	 * After all these processes, if there are still no tasks found, the <b><i>victim thread</i></b> will be set to <cod>null</code>
	 * and the method returns <code>null</code> indicating an unsuccessful attempt for polling a task.
	 * 
	 *  @author Mostafa Mehrabi
	 *  @since  14/9/2014
	 * */
	@Override
	public TaskID<?> workerPollNextTask() {
		
		WorkerThread wt = (WorkerThread) Thread.currentThread();
		TaskID<?> next = null;
		
		if (wt.isMultiTaskWorker()) {
			int workerID = wt.getThreadLocalID();
			
			next= privateQueues.get(workerID).poll();
			
			while (next != null) {
				if (next.executeAttempt()) {
					return next;
				} else {
					next.enqueueSlots(true);
					//-- task was successfully cancelled beforehand, therefore grab another task
					next = privateQueues.get(workerID).poll();
				}
			}
		}
			
		//if there were no tasks found in the privateQueue, then look into the globalMultiTask queue
		if (wt.isMultiTaskWorker()) {
			while ((next = globalMultiTaskqueue.poll()) != null) {
				// expand multi task
				int count = next.getCount();
				int currentMultiTaskThreadPool = ThreadPool.getMultiTaskThreadPoolSize();
				TaskInfo taskinfo = next.getTaskInfo();
				taskinfo.setSubTask(true);
				
				for (int i = 0; i < count; i++) {
					TaskID<?> taskID = new TaskID(taskinfo);
					taskID.setRelativeID(i);
					taskID.setExecuteOnThread(i%currentMultiTaskThreadPool);
					taskID.setSubTask(true);
					taskID.setPartOfGroup(((TaskIDGroup<?>)next));
					((TaskIDGroup<?>)next).add(taskID);
					enqueueReadyTask(taskID);
				}
				((TaskIDGroup<?>)next).setExpanded(true);
			}
		}
		//the worker thread is not a multi-task thread, then try to take from their
		//local one-off task queue (because the thread is a one-off task thread). 
		else {
			int workerID = wt.getThreadID();
			next = localOneoffTaskQueues.get(workerID).pollFirst();
			while (next != null) {
				if (next.executeAttempt()) {
					workFirstCounter.decrementAndGet();
					return next;
				} else {
					next.enqueueSlots(true);
					next = localOneoffTaskQueues.get(workerID).pollFirst();
				}
			}
			
			//if no task was found from the local one-off task queue
			//prefer to steal from the same victim last stolen from (if any)
			int prevVictim = lastStolenFrom.get();
			if (prevVictim != NOT_STOLEN) {
				Deque<TaskID<?>> victimQueue = localOneoffTaskQueues.get(prevVictim);
				if (null != victimQueue) {
					next = victimQueue.pollLast();
				}
				while (next != null) {
					if (next.executeAttempt()) {
						workFirstCounter.decrementAndGet();
						return next;
					} else {
						//-- task has been canceled
						next.enqueueSlots(true);
					}
					next = victimQueue.pollLast();	
				}
			}
			
			//no task could be stolen from the previous victim, so pick a new victim
			int oneoffTaskQueuesSize = localOneoffTaskQueues.size();
			int startVictim = (int) (Math.random()*oneoffTaskQueuesSize); 
			Integer[] workIDs = localOneoffTaskQueues.keySet().toArray(new Integer[oneoffTaskQueuesSize]);
		
			for (int v = 0; v < oneoffTaskQueuesSize; v++) {
				int nextVictim = workIDs[(startVictim+v)%oneoffTaskQueuesSize];
				//-- No point in trying to steal from self..
				if (nextVictim != workerID) {
					Deque<TaskID<?>> victimQueue = localOneoffTaskQueues.get(nextVictim);
					if (null != victimQueue) {
						next = victimQueue.pollLast();
					}

					while (next != null) {
						if (next.executeAttempt()) {
							lastStolenFrom.set(nextVictim);
							workFirstCounter.decrementAndGet();
							return next;
						} else {
							next.enqueueSlots(true);
						}
						next = victimQueue.pollLast();	
					}
				}
			}
			lastStolenFrom.set(NOT_STOLEN);
		}
		return null;
	}
	
	
	/**
	 * 	Assigns the threshold value that is used to determine the
	 * 	number of tasks to be enqueued, before Work-First is enforced.
	 * 	In TaskpoolLIFOWorkFirstGlobal, the setThreshold() and
	 * 	getThreshold() refers to the number of allowable tasks that
	 * 	can be enqueued in the task scheduler before it enforces
	 * 	Work-First on the system and directly processes the next task.
	 * 	The default value for the upperBoundThreshold is 70.
	 * 	@param threshold
	 */
	public static void setThreshold(int threshold) {
		upperBoundThreshold = threshold;
	}
	
	/**
	 * 	Returns the current threshold value set for the given task
	 * 	scheduler.
	 * 	In TaskpoolLIFOWorkFirstGlobal, the getThreshold() refers
	 * 	to the number of allowable tasks that the task scheduler can enqueue
	 * 	before Work-First is enforced on the system.
	 * 	@return
	 */
	public static int getThreshold() {
		return upperBoundThreshold;
	}
	
	/**
	 * 	Assigns the lower bound threshold value for the Global Task Population
	 * 	Control. This value is used to determine when it is possible for the
	 * 	task scheduler to resume enqueuing, when Work-First is currently enforced
	 * 	on the system.
	 * 	The default value for the lowerBoundThreshold is 10.
	 * 	@param threshold
	 */
	public static void setLowerBoundThreshold(int threshold) {
		lowerBoundThreshold = threshold;
	}
	
	/**
	 * 	Returns the value of the current lower bound threshold (lowerBoundThreshold)
	 * 	for the Global Task Population Control.
	 * 	@return
	 */
	public static int getLowerBoundThreshold() {
		return lowerBoundThreshold;
	}
}
