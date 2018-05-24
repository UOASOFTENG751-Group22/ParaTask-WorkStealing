/*
 *  Copyright (C) 2010 Nasser Giacaman, Oliver Sinnen
 *
 *  This file is part of Parallel Task.
 *
 *  Parallel Task is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at 
 *  your option) any later version.
 *
 *  Parallel Task is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 *  Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along 
 *  with Parallel Task. If not, see <http://www.gnu.org/licenses/>.
 */

package pt.runtime;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import pt.queues.PipelineQueue;

/**
 * A future object representing a task invocation. As well as containing the return result for non-<code>void</code> tasks, 
 * the <code>TaskID</code> may also be used with <code>dependsOn</code>, cancel attempts, and other various functions. 
 * <br><br>
 * Every aspect that deals with a task during its execution will go through this class <code>'TaskID'</code>. This class 
 * keeps the information regarding<br>
 * 1- A task's global ID and its relative ID (relativeID is used within a multi-task group).<br>
 * 2- The enclosing task of a specific instance (for finding asynchronous exceptions).<br>
 * 3- The initial information of this instance (i.e. taskInfo which is an instance of Task).<br>
 * 4- Indicates if a task can be executed by any arbitrary thread.<br>
 * 5- Records and returns the final result of a task.<br>
 * 6- Indicates if an instance of task has completed its execution.<br>
 * 7- Indicates if an instance of task has been requested to cancel.<br>
 * 8- Indicates if an instance of task has been successfully canceled (first cancel request, then practically cancel).<br>
 * 9- Keeps track of the progress of an instance of the task. <br>
 * 10-Returns the corresponding exception handler for a specific exception class by communicating with taskInfo<br> 
 * 11-Each instance has two count down latches. One for the registering thread, and one for other threads. Because<br>
 *    the registering thread is allowed to process through the slots, and we want to unblock the threads that are <br>
 *    waiting for the task to finish, before it starts proceeding through the slots (handlers)
 *<br><br>
 *Each task can have three states, <code>CREATED, CANCELLED</code> and <code>STARTED</code>. By default a task's status is 
 *set to CREATED, when its constructor is called. For a task to be executed or complete the execution it needs to be
 *on STARTED status. Whenever a task is created the count down latches will be set to one to block the threads that depend 
 *on this task, and when that task is completed, or cancelled the count down latches are set back to zero to unblock the 
 *threads.<br><br>
 *All sub-tasks of a multi-task share the same globalID, at the time of creation. A taskID holds the information about
 *whether a task is interactive, and whether it has slots (handlers) to notify from the task.<br><br>
 *Moreover, this task enables requesting for cancellation of the instance of task, allows recording the tasks that are
 *waiting for the instance of task to finish, returns the final result of a task and also provides a mechanism through 
 *which other threads can wait for the instance of task to finish (or do another task meanwhile they wait for the instance
 *of task to finish). For a task in order to complete all the slots stored in the task need to be executed; therefore the 
 *method 'enqueueSlots' must be called.      
 * 
 * 
 * @author Nasser Giacaman
 * @author Oliver Sinnen
 * @author Mostafa Mehrabi
 * 
 * @since  8/9/2014
 *
 * @param <E> The task's return type
 */
public class TaskID<E> {
	
	static protected AtomicInteger nextGlobalID = new AtomicInteger(-1);
	
	protected int globalID = -1;
	protected int relativeID = 0;
	
	// this is used in case we need to find an asynchronous exception handler
	protected TaskID<?> enclosingTask = null;	
	
	private int executeOnThread = ParaTaskHelper.ANY_THREAD_TASK;
	
	protected TaskInfo taskInfo = null;
	private E returnResult = null;
	
	private int progress = 0;

	protected AtomicBoolean hasCompleted = null;
	protected boolean cancelled = false;
	protected AtomicBoolean cancelRequested = new AtomicBoolean(false);
	
    // the registering thread has its own latch, since it is allowed to progress inside slots of this TaskID
    private CountDownLatch completedLatchForRegisteringThread= null;
    
    // all the other threads (non-registering threads) must wait at this latch, until slots complete
    private CountDownLatch completedLatch = null;
    
	private ReentrantLock changeStatusLock = null;
	
	protected AtomicBoolean hasUserError = new AtomicBoolean(false);
	private Throwable exception = null;
	
	private boolean isInteractive = false;
	
	protected ConcurrentLinkedQueue<TaskID<?>> waitingTasks = null;	// TaskIDs waiting for this task 
	protected ConcurrentHashMap<TaskID<?>, Object> remainingDependences = null;	// TaskIDs this task is waiting for
	
	protected TaskIDGroup<E> group = null;
	
	protected boolean hasSlots = false;

	static final protected int CREATED = 0;
	static final protected int CANCELLED = 1;
	static final protected int STARTED = 2;
	protected AtomicInteger status = new AtomicInteger(CREATED);
	
	// pipeline stuff
	private List<PipelineQueue<E>> outputQueues = new ArrayList<PipelineQueue<E>>();
	private boolean firstQueueClaimed = false;
	private PipelineThread pipelineThread = null; 
	
	/*
	 * 
	 * @Author  Kingsley
	 * @since 04/05/2013
	 * 
	 * Later Expansion
	 * Use this to indicate how many sub tasks should be expanded.
	 * Can only be set the value from {@link AbstractTaskPool#enqueueMulti()}
	 * 
	 * */
	private int count = 0;
	
	protected int getCount() {
		return count;
	}

	protected void setCount(int count) {
		this.count = count;
	}
	
	/*
	 * 
	 * @Author  Kingsley
	 * @since 21/05/2013
	 * 
	 * When a multi task is expanded, set this field to true for its every single sub tasks.
	 * 
	 * */
	private boolean isSubTask = false;

	protected boolean isSubTask() {
		return isSubTask;
	}

	protected void setSubTask(boolean isSubTask) {
		this.isSubTask = isSubTask;
	}
	
	
	/*
	 * 	@Author	Weng Hao
	 * 	
	 * 	Used to record the depth level for a task.
	 * 	Primarily used for the TaskpoolLIFOWorkFirstTaskDepth for the Task Depth Control.
	 * 	Default depth of a task is set to 1.
	 */
	
	private int taskDepth = 1;
	
	int getTaskDepth() {
		return taskDepth;
	}
	
	void setTaskDepth(int taskDepth) {
		this.taskDepth = taskDepth;
	}
	
	/*
	 * Checks to see if this task has successfully cancelled.
	 * @return <code>true</code> if it has cancelled successfully, <code>false</code> otherwise. 
	 */
	public boolean cancelledSuccessfully() {
		return cancelled;
	}
	
	/**
	 * Checks to see if this task is an interactibe task.
	 * 
	 * @return <code>true</code> if this is an interactive task, <code>false</code> otherwise.
	 */
	public boolean isInteractive() {
		return isInteractive;
	}
	
	/**
	 * Checks to see if this task if a part of a pipeline.
	 */
	public boolean isPipeline() {
		if (this.taskInfo == null)
			return false;
		else
			return taskInfo.isPipeline();
	}
	
	/**
	 * Assigns the thread for this stage of the pipeline. Used for cancelling
	 * the stage via PipelineThread.cancel().
	 */
	protected void setPipelineThread(PipelineThread pt) {
		if (!isPipeline()) 
			throw new IllegalStateException("trying to assign PipelineThread to non-pipeline task");
		
		this.pipelineThread = pt;
	}
	
	/**
	 * If this is a pipeline stage, get a queue from which all future results
	 * can be retrieved. If not, returns null.
	 * 
	 * Thread-safe.
	 */
	public BlockingQueue<E> getOutputQueue() {
		return getOutputQueue(null);
	}
	
	/**
	 * If this is a pipeline stage, get a queue from which all future results
	 * can be retrieved. If not, returns null.
	 * 
	 * Intended for internal use only. Must pass the TaskID of the task
	 * requesting the queue in order to associate the queue with the tasks it
	 * connects.
	 * 
	 * Thread-safe.
	 */
	protected BlockingQueue<E> getOutputQueue(TaskID requester) {
		if (!isPipeline())
			return null;
		
		synchronized(outputQueues) {
			if (!firstQueueClaimed) {
				firstQueueClaimed = true;
				if (outputQueues.size() == 0) {
					PipelineQueue<E> queue = new PipelineQueue<E>(this, requester);
					outputQueues.add(queue);
					return queue;
				} else {
					PipelineQueue<E> queue = outputQueues.get(0);
					queue.setTailTask(requester);
					return queue;
				}
			} else {
				PipelineQueue<E> queue = new PipelineQueue<E>(this, requester);
				outputQueues.add(queue);
				return queue;
			}
		}
	}
	
	/**
	 * Unregisters a queue from a pipeline stage so that new results are no
	 * longer written to it. Also, releases the reference so it can be GC'd.
	 * 
	 * For internal use only.
	 * 
	 * Thread-safe.
	 */
	protected void unregisterOutputQueue(PipelineQueue<E> queue) {
		if (!isPipeline()) {
			// maybe replace this with "return;"
			throw new IllegalStateException("trying to remove a queue from non-pipeline");
		}
		
		synchronized(outputQueues) {
			if (!outputQueues.contains(queue))
				throw new IllegalArgumentException(pipelineThread.getName() + ": queue to unregister not in outputQueues");
			
			outputQueues.remove(queue);
			
			// prevent this from being called again
			queue.setHeadTask(null);
			
			// reset firstQueueClaimed flag
			if (outputQueues.size() == 0)
				firstQueueClaimed = false;
		}
	}
	
	/**
	 * Write something into the output queue(s) handled by this TaskID. Only
	 * existing queues will get the object.
	 * 
	 * Thread-safe.
	 */
	protected void writeToOutputQueues(E value) {
		if (!isPipeline()) {
			// maybe replace this with "return;"
			throw new IllegalStateException("trying to write to output queue when not a pipeline");
		}
		
		synchronized(outputQueues) {
			if (outputQueues.size() == 0) {
				outputQueues.add(new PipelineQueue<E>(this, null));
			}
			
			for (PipelineQueue<E> queue : outputQueues) {
				queue.add(value);
			}
		}
	}
	
	/**
	 * For internal use only. Cancel all child stages if this is a pipeline.
	 */
	protected void cancelChildTasks() {
		if (!isPipeline()) {
			// maybe replace this with "return;"
			throw new IllegalStateException("trying to cancel child stages when not a pipeline");
		}
		
		synchronized(outputQueues) {
			for (PipelineQueue<E> queue : outputQueues) {
				if (queue.getTailTask() != null)
					queue.getTailTask().cancelAttempt();
			}
		}
	}
	
	void setProgress(int progress) {
		this.progress = progress;
	}

	public int getProgress() {
		return progress;
	}
	
	/**
	 * Checks to see if this task has been requested to cancel.
	 * @return <code>true</code> if it has been requested to cancel, <code>false</code> otherwise.
	 * @see CurrentTask#cancelRequested()
	 * @see #cancelAttempt()
	 * @see #cancelledSuccessfully()
	 */
	public boolean cancelRequested() {
		return cancelRequested.get();
	}
	
	TaskID(boolean alreadyCompleted) {
		if (alreadyCompleted) {
			globalID = nextGlobalID.incrementAndGet();
			completedLatch = new CountDownLatch(0);
			completedLatch = new CountDownLatch(0);
			hasCompleted = new AtomicBoolean(true);
			status = new AtomicInteger(STARTED);
		} else {
			throw new UnsupportedOperationException("Don't call this constructor if passing in 'false'!");
		}
	}
	
	/*
	 * 
	 * @author Kingsley
	 * @since 10/05/2013
	 * 
	 * Move the globalID allocation from the constructor of TaskID() 
	 * to the constructor of TaskID(TaskInfo taskInfo)
	 * 
	 * The idea is all subtasks of a multi task should share a global id,
	 * rather than give them a new one when they are created.
	 * */
	
	TaskID() {
		//globalID = nextGlobalID.incrementAndGet();
		completedLatch = new CountDownLatch(1);
		hasCompleted = new AtomicBoolean(false);
		status = new AtomicInteger(CREATED);
		changeStatusLock = new ReentrantLock();
	}
	
	/*
	 * This constructor receives information about, whether a task is interactive
	 * as well as it sets the count down latch to one. 
	 * */
	TaskID(TaskInfo taskInfo) {
		this();
		globalID = nextGlobalID.incrementAndGet();
		completedLatchForRegisteringThread = new CountDownLatch(1);
		this.taskInfo = taskInfo;
		isInteractive = taskInfo.isInteractive();
		if (taskInfo != null) {
			hasSlots = taskInfo.getSlotsToNotify() != null;
		}
	}
	
	/**
	 * Attempts to cancel the task. It first changes the state of the task to <code>CANCELLED</code>, and then
	 * checks if the previous status of the task was <code>CREATED</code> or if the task is already cancelled. 
	 * In that case the cancellation attempt will be successful, and the method will return <code>true</code>
	 * <br><br>
	 * If cancelled successfully, the task will not be enqueued. A failed cancel 
	 * will still allow the task to continue executing. To stop the task, the task should check
	 * to see if a cancel request has been made. 
	 * @return <code>true</code> if it has cancelled successfully, <code>false</code> otherwise.
	 *
	 * @author Mostafa Mehrabi
	 * @author Kingsley
	 * 
	 * @see #cancelRequested() 
	 * @see CurrentTask#cancelRequested()
	 * @see #cancelledSuccessfully()
	 */
	public boolean cancelAttempt() {
		cancelRequested.set(true);
		
		if (isPipeline()) {
			// this tells PipelineThread that some parent has requested cancel
			// it can be called more than once because of multiple parent stages
			pipelineThread.cancel();
			cancelled = true;
		}
		
		int prevStatus = status.getAndSet(CANCELLED);
		
		if (prevStatus == CREATED || cancelled) {
			cancelled = true;
			return true;
		}
		return false;
	}
	
	/**
	 * Returns the group that this task is part of (assuming it is a multi-task).  
	 * @return	Returns the group associated with this task. If not part of a multi-task, then
	 * returns <code>null</code>.
	 */
	public TaskIDGroup getGroup() {
		return group;
	}
	
	/**
	 * Tells if a task could start being executed. That means the task has been <code>CREATED</code>, and
	 * is not <code>CANCELLED</code>. Returns <code>true</code> if the task can start,
	 * and returns <code>false</code> otherwise.
	 * 
	 * @author Mostafa Mehrabi
	 * @since  9/9/2014
	 * */
	boolean executeAttempt() {
		int prevStatus = status.getAndSet(STARTED);
		return prevStatus == CREATED;
	}
	
	void setEnclosingTask(TaskID enclosingTask) {
		this.enclosingTask = enclosingTask;
	}
	
	TaskID getEnclosingTask() {
		return enclosingTask;
	}
	
	/**
	 * A <code>waiter</code> is another task that is waiting for the instance of task to finish. Once a 
	 * request for adding a waiter for this task is received, the instance will check if it is already 
	 * completed. If that is the case, the instance will remove itself from the list of dependences of 
	 * the <code>waiter</code>, otherwise the <code>waiter</code> will be added to the list of waiters.
	 * (i.e. list of other tasks which are waiting for this instance to finish).
	 * 
	 * @author Mostafa Mehrabi
	 * @since  9/9/2014
	 * */
	void addWaiter(TaskID<?> waiter) {
		if (hasCompleted.get()) {
			waiter.dependenceFinished(this);
		} else {
			changeStatusLock.lock();
			
			if (!hasCompleted.get()) {
				if (waitingTasks == null)
					waitingTasks = new ConcurrentLinkedQueue<TaskID<?>>();
				
				waitingTasks.add(waiter);
			} else {
				waiter.dependenceFinished(this);
			}
			changeStatusLock.unlock();
		}
	}
	
	/**
	 * Returns the (method) name of the task.
	 * @return
	 */
	public String getTaskName() {
		return taskInfo.getMethod().getName().replaceFirst(ParaTaskHelper.PT_PREFIX, "");
	}
	
	/** One of the other Tasks (that this task dependsOn) has finished, and 
	 * will be removed from the list of dependences of this task. If that was 
	 * the last Task in the list of dependences, the task pool (which is in
	 * charge of scheduling) will be informed that this instance of TaskID is 
	 * ready to be executed!
	 * 
	 *  @author Mostafa Mehrabi
	 *  @since  9/9/2014
	 * */
	void dependenceFinished(TaskID<?> otherTask) {
		remainingDependences.remove(otherTask);
		if (remainingDependences.isEmpty()) {
			TaskpoolFactory.getTaskpool().nowReady(this);
		}
	}
	
	void setRemainingDependences(ArrayList<TaskID<?>> deps) {
		remainingDependences = new ConcurrentHashMap<TaskID<?>, Object>();
		Iterator<TaskID<?>> it = deps.iterator();
		while (it.hasNext()) {
			remainingDependences.put(it.next(), "");
		}
	}
	
	/**
	 * Returns the task's globally-unique ID.
	 * @return	The task's unique ID.
	 * @see CurrentTask#globalID()
	 * @see CurrentTask#relativeID()
	 * @see #relativeID()
	 */
	public int globalID() {
		return globalID;
	}
	
	/**
	 * Returns the sub-task's relative ID in the multi-task. 
	 * @return	The position, starting from 0, of this sub-task compared to it's sibling subtasks.
	 * @see CurrentTask#globalID()
	 * @see CurrentTask#relativeID()
	 * @see #globalID()  
	 */
	public int relativeID() {
		return relativeID;
	}
	
	void setRelativeID(int relativeID) {
		this.relativeID = relativeID;
	}
	
	TaskInfo getTaskInfo() {
		return taskInfo;
	}
	
	/**
	 * Returns the result of the task. If the task has not finished yet, the current thread blocks. 
	 * ParaTask worker threads will not block, instead they execute other ready tasks until
	 * this task completes.
	 * @return	The result of the task.
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public E getReturnResult() throws ExecutionException, InterruptedException {
		waitTillFinished();
		if (cancelledSuccessfully())
			throw new ParaTaskRuntimeException("Attempting to get the result of a cancelled Task!");
		return returnResult;
	}
	
	/**
	 * Helps the current thread with waiting for the task to finish. This method first
	 * checks if the task is already completed. If not, the method starts dealing with
	 * the current thread. Interactive threads or user threads can block and wait until
	 * the task is finished. However, if the blocking thread is a ParaTask worker thread, 
	 * it can execute some other tasks until this task completes. 
	 * <br><br>
	 * Before a worker tries to find another task and executes it, this thread should 
	 * check if there is a cancel request for the thread. If a worker thread is poisoned
	 * (i.e. requested to cancel, but not cancelled yet), even if there are some unfinished
	 * children tasks, it will not get a chance to execute them, but it will have a chance
	 * to inform the task pool and ask the task pool to remove it (i.e. the current worker 
	 * thread) from its (task pool's) list of worker threads.
	 * <br><br>
	 * If the current thread is a worker thread and is not poisoned, neither it is cancelled 
	 * already, it will be allowed to execute some other tasks (depending on the scheduling 
	 * scheme) or it can go to sleep. If the thread is logically cancelled already, it will 
	 * sleep until it is shut down by the virtual machine.
	 * <br><br>
	 * If the current thread is not a worker thread, and is the thread that as registered this
	 * task it has to wait on its own count down latch, other wise the thread can wait on the 
	 * normal count down latch.
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * 
	 * @author Mostafa Mehrabi
	 * @since  9/9/2014
	 * @author Kingsley
	 * @since 25/05/2013
	 * */
	public void waitTillFinished() throws ExecutionException, InterruptedException {		
		if (!hasCompleted.get()) {
			Thread t = Thread.currentThread();
			
			/* Only WorkerThreads should start a new TaskID.. all other threads belong to the user, or 
			 * are InteractiveThreads (therefore it is OK for them to block) */
			if (t instanceof WorkerThread) {
				WorkerThread currentWorker = (WorkerThread) t;
				while (!hasCompleted.get()) {
					if (currentWorker.isCancelRequired() && !currentWorker.isCancelled()) {
						LottoBox.tryLuck();
					}
					
					if (!currentWorker.isCancelled()) {
						currentWorker.executeAnotherTaskOrSleep();
					} else {
						try {
							Thread.sleep(ParaTask.WORKER_SLEEP_DELAY);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
				}
			} else {
				if (currentThreadIsTheRegisteredThread()) {
					completedLatchForRegisteringThread.await();
				} else {
					completedLatch.await();
				}
			}
		}
		
		//-- task has completed.. was there a user error?
		if (hasUserError.get()) {
			throw new ExecutionException(exception);
		}
	}
	
	protected boolean currentThreadIsTheRegisteredThread() {
		Thread registered = taskInfo.getRegisteringThread();
		if (registered == null)
			return false;
		if (registered == ParaTask.getEDT() && GuiThread.isEventDispatchThread())
			return true;
		else 
			return registered == Thread.currentThread();
	}
	
	void setReturnResult(E returnResult) {
		this.returnResult = returnResult;
	}
	
	/**
	 * Returns the exception that occurred while this task executed. 
	 * @return	The exception that occurred.
	 */
	public Throwable getException() {
		return exception;
	}
	
	void setException(Throwable exception) {
		this.exception = exception;
		hasUserError.set(true);
		if (group != null) {
			group.setException(exception);
		}
	}
	
	/**
	 * Checks to see whether the task has completed.
	 * @return	<code>true</code> if it has completed, <code>false</code> otherwise
	 * @see #getProgress()
	 * @see CurrentTask#getProgress()
	 */
	public boolean hasCompleted() {
		return hasCompleted.get();
	}

	/**
	 * Checks to see whether the task had any errors. 
	 * @return	<code>true</code> if there was an error, <code>false</code> otherwise
	 */
	public boolean hasUserError() {
		return hasUserError.get();
	}
	
	/**
	 * Invoke all the dependent subtasks for group
	 * 
	 * This method is called when a task is complete. In order to set a task as "complete", this
	 * method goes through the list of tasks that have been waiting for this task to finish, and
	 * removes the task from their lists of dependences. Then it unblocks all the waiting threads,
	 * even the registering thread (in case there are slots to execute), and sets the flag 'hasComplete' 
	 * to <code>true</code>.
	 * If this task is registered as a group of tasks, the method will go through all tasks that have 
	 * been waiting for this group to finish, and then removes this group from their lists of dependences.
	 * If any of those waiting tasks have their list of dependences emptied by removing this group, it will
	 * be introduced to the task pool as a ready-to-execute task. 
	 * 
	 * @author Mostafa Mehrabi
	 * @since 9/9/2014
	 * */
	void setComplete() {
		
		changeStatusLock.lock();
		TaskID<?> waiter = null;
		
		if (waitingTasks != null) {
			while ((waiter = waitingTasks.poll()) != null) {
				// removes the waiter from the queue
				waiter.dependenceFinished(this);
			}
		}
		
		completedLatchForRegisteringThread.countDown();	//-- in case there were slots
		completedLatch.countDown();
		hasCompleted.set(true);
		changeStatusLock.unlock();
		
	}
	

	/**
	 * In order to make sure a task is completed, the possible handlers (slots) that are queued by the task need 
	 * to be invoked. This method invokes slots and the exception handlers to be executed by the registered thread. 
	 * <br><br>
	 * Note that this TaskID is NOT considered completed, until all these slots are finished (even though the 
	 * actual task logic has been executed, we need to wait for the slots before handling dependences, etc).
	 * Therefore, the registering thread will later set the status of this task as complete. 
	 *
	 *@author Mostafa Mehrabi
	 *@since  9/9/2014
	 **/
	void enqueueSlots(boolean onlyEnqueueFinishedSlot) {
		
		if (group != null && group.isMultiTask()) {
			//part of a multi-task, will only enqueue the slots of the group when the last TaskID in the group completes
			//however, this specific sub-task should be set as complete to release the threads that are waiting on it. 
			group.oneMoreInnerTaskCompleted();
			setComplete();	
		} else {
			if (hasUserError() || hasSlots) {
				//so that registering thread will not block in slots
				completedLatchForRegisteringThread.countDown();  
				completedLatch.countDown();
				
				if (hasUserError.get())
					executeHandlers();
				if (hasSlots)
					executeSlots();
				
				//-- 		since slots are executed in the order they are enqueued, then this will be the last slot! :-)
				callTaskListener(new Slot(ParaTaskHelper.setCompleteSlot, this, false, Slot.SetCompleteSlot.TRUE));
				
			} else {
				setComplete();
			}
		}
	}
	
	/**
	 * Returns the appropriate exception hanlder for a specific class of exception,
	 * by receiving that exception class as argument.
	 * 
	 * @author Mostafa Mehrabi
	 * @since 9/9/2014
	 * */
	protected Slot getExceptionHandler(Class occurredException) {
		
		//-- first, try to get handler defined immediately for this task
		Slot handler = taskInfo.getExceptionHandler(occurredException);
		
		TaskID<?> curTask = this;
		//-- while we have not found a handler, and while there are other enclosing tasks
		while (handler == null && curTask.getEnclosingTask() != null) {
			curTask = curTask.getEnclosingTask();
			handler = curTask.getTaskInfo().getExceptionHandler(occurredException);
		}
		
		return handler;
	}
	
	//-- returns the number of handlers that it will execute for this TaskID
	private int executeHandlers() {
		Slot handler = getExceptionHandler(exception.getClass());
		
		if (handler != null) {
			callTaskListener(handler);
			return 1;
		} else {
			String taskName = "";
			Method method = taskInfo.getMethod();
			if (method.getDeclaringClass().getPackage() != null)
				taskName+=method.getDeclaringClass().getPackage().getName()+".";
			taskName+=getTaskName()+"(";
			Class[] params = method.getParameterTypes();
			if (params != null) {
				for (int p = 0; p < params.length; p++) {
					taskName+=params[p].getName();
					if (p != (params.length-1))
						taskName+=", ";
				}
			}
			taskName+=")";
			System.err.println("No asynchronous exception handler (i.e. asyncCatch clause) was specified when " +
					"invoking:\n\t\t "+taskName +" " +
							"\n\tThe globalID of this task is "+ globalID() + ", and the encountered exception: ");
			exception.printStackTrace();
			return 0;
		}
	}
	
	void callTaskListener(Slot slot) {
		ParaTask.getEDTTaskListener().executeSlot(slot);
	}
	
	protected int executeIntermediateSlots() {
		return 0;
	}
	
	protected int executeSlots() {
		for (Iterator<Slot> it = taskInfo.getSlotsToNotify().iterator(); it.hasNext(); ) 
			callTaskListener(it.next());
		return taskInfo.getSlotsToNotify().size();
	}
	
	/**
	 * Returns the arguments that were passed to this task when it was invoked.  
	 * 
	 * @return The arguments initially passed to the task when it was invoked
	 */
	public Object[] getTaskArguments() {
		return taskInfo.getParameters();
	}
	
	void setPartOfGroup(TaskIDGroup group) {
		this.group = group;
	}
	
	/**
	 * Checks to see if this task is part of a multi-task group.
	 * @return <code>true</code> if this task is part of a multi-task, <code>false</code> otherwise 
	 */
	public boolean isMultiTask() {
		return group != null;
	}
	
	/**
	 * Returns the size of the multi-task this task is part of. 
	 * @return	The multi-task size, otherwise returns 1 if this task is not part of a multi-task.
	 */
	public int multiTaskSize() {
		if (group == null)
			return 1;
		return group.groupSize();
	}
	
	int getExecuteOnThread() {
		return executeOnThread;
	}
	
	void setExecuteOnThread(int executeOnThread) {
		this.executeOnThread = executeOnThread;
	}

	
	/**
	 * 
	 * @Author : Kingsley
	 * @since : 10/05/2013
	 * 
	 * When multi task is expanded, call this method, and set global id for its sub tasks.
	 * 
	 *
	 * */
	protected void setGlobalID(int globalID) {
		this.globalID = globalID;
	}
	
	
}
