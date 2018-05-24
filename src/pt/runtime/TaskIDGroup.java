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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import pu.RedLib.Reduction;

/**
 * An extension of <code>TaskID</code> to contain multiple tasks. In particular, a <code>TaskIDGroup</code> is returned for 
 * all multi-task invocations. Users may also instantiate a <code>TaskIDGroup</code> and populate it with multiple 
 * <code>TaskID</code>s (and <code>TaskIDGroup</code>s). This would be useful, for example, when invoking many different 
 * tasks, allowing a collective approach to synchronise on those tasks (i.e. wait on the <code>TaskIDGroup</code> rather
 * than the on each individual <code>TaskID</code>. 
 * 
 * @author Nasser Giacaman
 * @author Oliver Sinnen
 *
 * @param <E>
 */
public class TaskIDGroup<E> extends TaskID<E> {
	
	private ArrayList<TaskID<?>> innerTasks = new ArrayList<TaskID<?>>();
	
	private AtomicInteger numTaskCompleted = new AtomicInteger(0);
	
	private AtomicInteger barrier = new AtomicInteger(0);
	
	//-- Reductions are only performed once at most, by a single thread
	private boolean performedReduction = false;
	private ReentrantLock reductionLock = new ReentrantLock();
	private E reductionAnswer;
	
	private int nextRelativeID = 0;
	
	private int groupSize = 0;
	
	//only a multi-task if the group was created by ParaTask in a TASK(*) declaration
	//this distinguishes it from user-created groups
	private boolean isMultiTask = false;  
	
	private ParaTaskExceptionGroup exceptionGroup = null;
	private CopyOnWriteArrayList<Throwable> exceptionList = new CopyOnWriteArrayList<Throwable>();
	
	/*
	 *  This is used to indicate if the multi task has been expanded or not
	 */
	private boolean isExpanded = false;
	
	/**
	 * This public constructor is actually used to group a bunch of tasks, which
	 * may include one-off task or multi task, should not give any id to this 
	 * group.
	 * */
	public TaskIDGroup(int groupSize) {
		this.groupSize = groupSize;
	}
	
	//-- this is only used to create a multi-task (the size is known before adding the inner tasks)
	TaskIDGroup(int groupSize, TaskInfo taskInfo) {
		super(taskInfo);
		this.isMultiTask = true;
		this.groupSize = groupSize;
		this.taskInfo = taskInfo;
	}
	
	/**
	 * Checks whether this TaskIDGroup represents a multi-task. This is because users may
	 * use <code>TaskIDGroup</code> to group a set of <code>TaskID</code>s, but those <code>TaskID</code>s might not necessarily
	 * be part of a multi-task.
	 * @return	<code>true</code> if this <code>TaskIDGroup</code> represents an actual multi-task, <code>false</code> otherwise
	 */
	public boolean isMultiTask() {
		return isMultiTask;
	}
	
	public void add(TaskID<?> id) {
		innerTasks.add(id);
	}
	
	/**
	 * Returns the group size.
	 * @return	The group size.
	 */
	public int groupSize() {
		return groupSize;
	}
	
	/**
	 * Perform a reduction on the set of results. A reduction is only to be performed once. 
	 * If this is called a second time then the pre-calculated answer is returned.
	 * @param red	The reduction to perform
	 * @return The result of performing the reduction on the set of <code>TaskID</code>s contained in this group.
	 */
	public E reduce(Reduction<E> red) throws ExecutionException, InterruptedException {
		waitTillFinished();
		
		// TODO want to make this like the Parallel Iterator's reduction.. i.e. checks initial value, etc.. 
		
		if (groupSize == 0)
			return null;
		
		reductionLock.lock();
		if (performedReduction) {
			reductionLock.unlock();
			return reductionAnswer;
		}
		reductionAnswer = getInnerTaskResult(0);
		for (int i = 1; i < groupSize; i++) {
			reductionAnswer = red.reduce(reductionAnswer, getInnerTaskResult(i));
		}
		performedReduction = true;
		reductionLock.unlock();
		return reductionAnswer;
	}
	
	/**
	 * Returns the result of a particular task.
	 * @param relativeID The relative ID of the task whose result is wanted.
	 * @see CurrentTask#relativeID()
	 * @see TaskID#relativeID()
	 * @return The result for that task.
	 */
	public E getInnerTaskResult(int relativeID) throws ExecutionException, InterruptedException {
		return (E) innerTasks.get(relativeID).getReturnResult();
	}
	
	/**
	 * Return an iterator for the set of <code>TaskID</code>s contained in this group.
	 * @return	An iterator for this group of TaskIDs.
	 */
	public Iterator<TaskID<?>> groupMembers() {
		return innerTasks.iterator();
	}
	
	/**
	 * Increments the number of inner tasks that have finished executing. Then checks if all inner-tasks
	 * are completed. If that is the case, then checks if there are any exceptions asynchronously recorded
	 * for any of the inner-tasks, and calls their handlers. Moreover, it checks for slots to notify   
	 * and executes them. Then it sets the task as "complete".
	 * 
	 * @author Mostafa Mehrabi
	 * @since  9/9/2014
	 */
	void oneMoreInnerTaskCompleted() { 
		int numCompleted = numTaskCompleted.incrementAndGet();
		
		if (groupSize == numCompleted) {
			//-- this is the last task in the multi-task group, therefore need to invoke slots/handlers
			boolean nothingToQueue = true;
			
			if (hasUserError()) {
			for (Iterator<TaskID<?>> it = groupMembers(); it.hasNext(); ) {
					TaskID<?> task = it.next();
					Throwable ex = task.getException();
					if (ex != null) {
						Slot handler = getExceptionHandler(ex.getClass());
						
						if (handler != null) {
							callTaskListener(handler);
							nothingToQueue = false;
						} else {
							System.err.println("No asynchronous exception handler found in Task " + task.globalID() + " for the following exception: ");
							ex.printStackTrace();
						}
					}
				}
			}
			
			//-- executeSlots
			if (hasSlots) {
				executeSlots();
				nothingToQueue = false;
			} else {
			}

			if (nothingToQueue) {
				setComplete();
			} else {
				callTaskListener(new Slot(ParaTaskHelper.setCompleteSlot, this, false, Slot.SetCompleteSlot.TRUE));
			}
		} 
	}

	@Override
	public Throwable getException() {
		return exceptionGroup;
	}

	@Override
	public E getReturnResult() throws ExecutionException, InterruptedException {
		throw new UnsupportedOperationException("This is a TaskIDGroup, you must either specify a Reduction or get individual results from the inner TaskID members.");
	}

	public E getReturnResult(Reduction<E> red) throws ExecutionException, InterruptedException {
		return reduce(red);
	}

	@Override
	TaskInfo getTaskInfo() {
		return taskInfo;
	}

	/**
	 * Checks to see whether all the inner tasks have completed.
	 */
	@Override
	public boolean hasCompleted() {
		return super.hasCompleted();
	}
	
	/**
	 * Checks to see whether any of the inner tasks contained an error. 
	 */
	@Override
	public boolean hasUserError() {
		return super.hasUserError();
	}
	
	@Override
	void setException(Throwable exception) {
		exceptionList.add(exception);
		hasUserError.set(true);
	}

	@Override
	void setReturnResult(Object returnResult) {
		throw new ParaTaskRuntimeException("Cannot set the return result for a TaskIDGroup");
	}

	/**
	 * Canceling a group is currently not supported. Users must cancel each inner task individually.
	 * @throws UnsupportedOperationException	
	 */
	@Override
	public boolean cancelAttempt() {
		throw new UnsupportedOperationException("Cancelling a group is currently not supported, must cancel inner tasks individually.");
	}
	
	/**
	 * Waits for all the contained inner tasks to complete. It goes through all inner-tasks and
	 * if an inner-task is a TaskIDGroup, which means there is another multi-task inside the group
	 * waits until that inner multi-task is expanded and all its task are finished. Then, it proceeds 
	 * to finishe other inner-tasks.
	 * If an inner-task is a TaskID, which means it is a normal task, wait until that task is completed.
	 *
	 * @author Kingsley
	 * @author Mostafa Mehrabi
     * @since 08/05/2013
     * @since 9/9/2014
	 * */
	@Override
	public void waitTillFinished() throws ExecutionException, InterruptedException {
		if(isMultiTask()){
			while(!isExpanded)
				Thread.sleep(ParaTask.WORKER_SLEEP_DELAY);
		}
		int size = innerTasks.size();
		for (int i = size-1; i >= 0; i--) {// wait for them in reverse order (LIFO)
			try {
				TaskID<?> taskID= innerTasks.get(i);
				if (taskID instanceof TaskIDGroup) {
					TaskIDGroup<?> taskIDGroup = (TaskIDGroup<?>) taskID;
					while (!taskIDGroup.getExpanded()) {
						Thread.sleep(ParaTask.WORKER_SLEEP_DELAY);
					}
					taskIDGroup.waitTillFinished();
				} else {
					taskID.waitTillFinished();
				}
			} catch (ExecutionException e) {
				this.setException(e);
			}
		}
		if (hasUserError.get()) {
			String reason = "Exception(s) occured inside multi-task execution (GlobalID of "+globalID+"). Individual exceptions are accessed via getExceptionSet()";
			exceptionGroup = new ParaTaskExceptionGroup(reason, exceptionList.toArray(new Throwable[0]));
			throw exceptionGroup;
		}
	}
	
	/**
	 * This method sets a checkpoint at which threads that arrive earlier wait until all threads arrive. 
	 * This is mostly done in situations where we want to make sure that at a specific stage
	 * all threads have reached a specific point in the program. 
	 * <br>
	 * Therefore, while none of the threads have called <code>barrier()</code>, all threads carry on doing their
	 * ordinary tasks. Once <code>barrier()</code> is called by a thread, that thread has to wait (either doing 
	 * some other tasks, or going to sleep) until all other threads arrive to that check point (i.e. call 
	 * <code>barrier()</code>). When all threads have called <code>barrier()</code>, barrier's counter will
	 * be set back to <b>zero</b>, and threads can carry on doing their tasks.
	 * 
	 * @author Mostafa Mehrabi
	 * @since  9/9/2014
	 * */
	void barrier() throws InterruptedException, BrokenBarrierException {
		int pos = barrier.incrementAndGet();
		WorkerThread currentWorker = (WorkerThread) Thread.currentThread();
		
		if (pos != groupSize) {
			while (barrier.get() != groupSize && barrier.get() != 0) {
				//-- keep executing other tasks until all the threads have reached the barrier
				currentWorker.executeAnotherTaskOrSleep();
			}
		} else {
			//-- this is the last thread to arrive.. reset the barrier
			barrier.set(0);
		}
	}


	/**
	 * 
	 * @author Kingsley
	 * @since 08/05/2013
	 * 
	 * After a multi task worker thread expand a mult task, call this method to set a "true" value.
	 */
	protected void setExpanded(boolean isExpanded) {
		this.isExpanded = isExpanded;
	}
	
	protected boolean getExpanded(){
		return isExpanded;
	}
}
