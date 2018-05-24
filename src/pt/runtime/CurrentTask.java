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
import java.util.concurrent.BrokenBarrierException;

/**
 * Helper methods for the currently executing task. This class contains various functions in regards
 * to the current task. 
 * <br><br>
 * The methods in this class are only applicable to the currently executed task, and will therefore 
 * throw a <code>RuntimeException</code> when called from a non-task. To inquire if within a task, programmers 
 * may use {@link #insideTask()}. 
 * 
 * @author Nasser Giacaman
 * @author Oliver Sinnen
 *
 */
public class CurrentTask {
	
	CurrentTask() {
	}
	
    /**
     * Returns the ParaTask thread's ID. This method is only applicable for ParaTask threads, and must therefore 
     * only be called within ParaTask tasks. 
     * @return	The ParaTask thread's ID.
     * 
     * @throws RuntimeException if not called from within a ParaTask task. 
     */
	public static int currentThreadID() {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.currentThreadID() may only be called from within a Task");
		}
		return ((TaskThread)t).getThreadID();
	}
	
	 /**
	  * 
	  * @author Kingsley
	  * @since 10/05/2013
	  * 
      * Returns the ParaTask thread's Local ID. This method is only applicable for ParaTask threads, and must therefore 
      * only be called within ParaTask tasks. 
      * @return	The ParaTask thread's Local ID.
      * 
      * @throws RuntimeException if not called from within a ParaTask task. 
     */
	public static int currentThreadLocalID() {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.currentThreadID() may only be called from within a Task");
		}
		return ((TaskThread)t).getThreadLocalID();
	}
	
	/**
	 * Inquire as to whether the current code being executed is inside a task. Useful in some applications where
	 * the same code fragment might be executed both as a task and sequentially. For example: 
	 * <br><br>
	 * <code><pre>
	 * 
	 * TASK int computeTask(int value) {
	 * 		return compute(value); 
	 * }
	 * 
	 * public int compute(int value) {
	 * 		...
	 * 		if (ParaTask.insideTask()) {
	 *		 	// this method is being executed as a ParaTask task
	 * 		} else {
	 * 			// this method is not being executed as a ParaTask task
	 * 			// (maybe the event dispatch thread, main thread, or another user-defined thread)   
	 * 		}
	 * 		...
	 * }
	 * 
	 * </pre></code>
	 * 
	 * @return <code>true</code> if the currently inside a task, <code>false</code> otherwise.
	 */
	public static boolean insideTask() {
		return Thread.currentThread() instanceof TaskThread;
	}
	

	/**
	 * Returns the current task's multi-task size. i.e. the number of sub-tasks for the current sub-task's multi-task.
	 * If the current task is not part of a multi-task, then 1 is always returned.  
	 * @return	The size of the current task's group.
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static int multiTaskSize() {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.multiTaskSize() may only be called from within a Task");
		}
		return ((TaskThread)t).currentExecutingTask().multiTaskSize();
	}
	

	/**
	 * Returns the progress of the currently executed task.
	 *   
	 * @return	The progress, as updated by the task.
	 * @see TaskID#setProgress(int) 
	 * @see #setProgress(int)
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static int getProgress() {
		return currentTaskID().getProgress();
	}
	
	/**
	 * Updates the progress of the currently executing task.
	 * 
	 * @param progress	The new progress of the currently executing task.
	 * 
	 * @see TaskID#getProgress() 
	 * @see #getProgress()
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static void setProgress(int progress) {
		currentTaskID().setProgress(progress);
	}
	
	/**
	 * Return the current TaskID associated with the currently executing task. 
	 * @return	The current task's TaskID
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static TaskID currentTaskID() {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.currentTaskID() may only be called from within a Task");
		}
		return ((TaskThread)t).currentExecutingTask();
	}
	
	/**
	 * Returns the current task's global ID. All tasks have a unique ID, and this is it.
	 * @return	The current task's globally-unique ID
	 * @see TaskID#globalID()
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static int globalID() {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.globalID() may only be called from within a Task");
		}
		return ((TaskThread)t).currentExecutingTask().globalID();
	}
	
	/**
	 * All tasks have a relative ID. But this only makes sense for multi-tasks. If the current task is not 
	 * part of a multi-task, then this always returns 0. Relative IDs start from 0.
	 * @return	The current task's relative ID
	 * @see TaskID#relativeID()
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static int relativeID() {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.relativeID() may only be called from within a Task");
		}
		return ((TaskThread)t).currentExecutingTask().relativeID();
	}
	
	/**
	 * The current task checks to see if it has been requested to cancel. 
     * @throws RuntimeException if not called from within a ParaTask task. 
     * @see TaskID#cancelAttempt()
     * @see TaskID#cancelRequested() 
	 * @return	<code>true</code> if the current task has been asked to cancel, <code>false</code> otherwise.
	 */
	public static boolean cancelRequested() {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.cancelRequested() may only be called from within a Task");
		}
		return ((TaskThread)t).currentExecutingTask().cancelRequested();
	}
	
	/**
	 * Publish intermediate results. If any slots are registered to listen for interim results 
	 * (i.e. using either the <code>notifyInterim</code> or <code>notifyInterimGUI</code> clause), this is the method to publish those
	 * results to them.
	 * @param <E>	The type of the interim result to be published 
	 * @param interimResult		The interim result being published.
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static <E>void publishInterim(E interimResult) {
		TaskID<?> id = CurrentTask.currentTaskID();
		ArrayList<Slot> interSlots = id.getTaskInfo().getInterSlotsToNotify() ;
		if (interSlots == null)
			return;
		for (Slot s : interSlots) {
			s.addIntermediateResult(interimResult.getClass(), interimResult);
			id.callTaskListener(s);
		}
	}
	
	
	/**
	 * Barrier synchronisation for multi-tasks. To avoid deadlock, it is important that all sub-tasks of the
	 * multi-task call this method, otherwise sub-tasks will continue to wait and never complete! This is safe 
	 * to call recursively and by multiple multi-tasks, since a "waiting" worker thread will in fact execute other
	 * ready tasks while it waits for the sibling sub-tasks to also reach the barrier.
	 * @throws InterruptedException
	 * @throws BrokenBarrierException
     * @throws RuntimeException if not called from within a ParaTask task. 
	 */
	public static void barrier() throws InterruptedException, BrokenBarrierException {
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.barrier() may only be called from within a (Multi-)Task");
		}
		TaskID ct = ((TaskThread)t).currentExecutingTask();
		if (ct.isMultiTask()) {
			ct.getGroup().barrier();
		} else {
			//-- no need for barrier since there are no siblings
		}
	}
	
	
	/**
	 * @author Kingsley
	 * @since 21/05/2013
	 * @return true, if current task is one-off task. Otherwise false
	 * 
	 * Check if current executing task is one-off task.
	 * If current thread is one-off task thread, its current task is definitely one-off task.
	 * 
	 * */
	public static boolean isOneoffTask(){
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.isOneoffTask() may only be called from within a Task");
		}
		return ((TaskThread)t).getThreadLocalID() == -1? true : false;
	}
	
	/**
	 * @author Kingsley
	 * @since 21/05/2013
	 * @return true, if current task is multi task. Otherwise false
	 * 
	 * Check if current executing task is multi task.
	 * If current thread is multi task thread, its current task is definitely multi task.
	 * 
	 * */
	public static boolean isMultiTask(){
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.isMultiTask() may only be called from within a Task");
		}
		return ((TaskThread)t).getThreadLocalID() != -1? true : false;
	}
	
	/**
	 * @author Kingsley
	 * @since 21/05/2013
	 * @return true, if current task is sub task. Otherwise false
	 * 
	 * Check if current executing task is sub task.
	 * 
	 * */
	public static boolean isSubTask(){
		Thread t = Thread.currentThread();
		if (!(t instanceof TaskThread)) {
			throw new ParaTaskRuntimeException("ParaTask.isSubTask() may only be called from within a Task");
		}
	
		return ((TaskThread)t).currentExecutingTask().isSubTask();
	}
}
