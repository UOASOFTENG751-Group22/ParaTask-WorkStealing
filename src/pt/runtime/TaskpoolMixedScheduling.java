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

import java.util.AbstractQueue;
import java.util.ArrayList;
import pt.queues.FifoLifoQueue;

/**
 * The mixed scheduling policy is a combination of <code>Work Stealing</code> and <code>Work Sharing</code> policies,
 * such that when enqueuing a task, if the enqueuing thread is a <i>worker thread</i>, the <code><b>Work Stealing</b></code>
 * policy is used, and if the enqueuing thread is a <i>non-worker thread</i>, the <code><b>Work Sharing</b></code> policy is
 * used!
 * <br><br>
 * A worker thread potentially prefers to execute tasks from its own <code>localOneOffTask</code> queue before helping with 
 * the global shared queue!
 * 
 * @author Mostafa Mehrabi
 * @since  14/9/2014
 * */
public class TaskpoolMixedScheduling extends AbstractTaskPool {
   /**
	 * When enqueuing tasks under the <code>Mixed Scheduling</code> policy, if a task <b>cannot</b> be executed 
	 * by arbitrary threads, it will be enqueued to the <code>privateQueue</code> of the thread in charge of
	 * executing it. 
	 * <br><br>
	 * However, if the task can be executed by arbitrary threads it will be enqueued to the <code>mixedMultiTask</code>
	 * queue if it is a <code>TaskIDGroup</code>, otherwise it will be enqueued to the <code>mixedOneOffTask</code> queue.
	 * 
	 * @author Mostafa Mehrabi
	 * @sice   14/9/2014
	 * */
	@Override
	protected void enqueueReadyTask(TaskID<?> taskID) {
			
		if (taskID.getExecuteOnThread() != ParaTaskHelper.ANY_THREAD_TASK || taskID instanceof TaskIDGroup) {
			if (taskID.getExecuteOnThread() == ParaTaskHelper.ANY_THREAD_TASK) {
				mixedMultiTaskqueue.addGlobal(taskID);
			} else {
				privateQueues.get(taskID.getExecuteOnThread()).add(taskID);
			}
		}else {
			if (Thread.currentThread() instanceof TaskThread) {
				mixedOneoffTaskqueue.addLocal(taskID);
			}else {
				mixedOneoffTaskqueue.addGlobal(taskID);
			}
		}
	}
	
	/**
	 * This method polls a new task for the current thread that is requesting for a new task under the 
	 * <code>Mixed Scheduling</code> policy. This method should only be called by <code>Worker Threads</code>. 
	 * <br><br>
	 * Once a worker thread requests for a new task, this method checks if the thread is a <code>Multi Task</code>
	 * thread. If so, then the <code>privateQueue</code> of that thread will be checked. In case there is a 
	 * task found which passes the preliminary execution attempt successfully, the task is passed to the thread to execute.
	 * <br><br>
	 * However, if the private queue of the <code>Multi Task</code> worker thread does not have any executable tasks, then 
	 * the method will check the <code>mixedMultiTask</code> queue. Every multi-task in this queue will be expanded into its
	 * sub-tasks, and the sub-tasks will be enqueue as <code>ready-to-execute</code> tasks, but the thread will temporarily 
	 * return <b>without</b> a task for this time, waiting for later chances. 
	 * <br><br>
	 * If the current worker thread is not a <code>Multi Task</code> worker thread, then the method checks the 
	 * <code>mixedOneOffTask</code> queue. For a given task inside this queue, if the thread is allowed to execute it, the 
	 * task will be checked with preliminary execution attempt. If the attempt is successful, the task will be sent to thread
	 * to execute. However, if the thread is not allowed to execute the task, the task will be enqueued to the <code>privateQueue</code>
	 * of the thread in charge of executing it.This process will continue until either a task is found for the thread, or all
	 * tasks inside the <code>mixedOneOffTask</code> queue are polled and enqueued into <code>privateQueues</code>.
	 * 
	 *@author Mostafa Mehrabi
	 *@since  14/9/2014
	 * */
	@Override
	public TaskID<?> workerPollNextTask() {
		
		WorkerThread wt = (WorkerThread) Thread.currentThread();
		int workerID = wt.getThreadID();
		TaskID<?> next = null;
		
		if (wt.isMultiTaskWorker()) {
			next = privateQueues.get(workerID).poll();
			while (next != null) {
				if (next.executeAttempt()) {
					return next;
				} else {
					next.enqueueSlots(true);	//-- task is considered complete, so execute slots
				}
				next = privateQueues.get(workerID).poll();
			}
		}
		
		if (wt.isMultiTaskWorker()) {
			while ((next = mixedMultiTaskqueue.poll()) != null) {
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
		}else {
			while ((next = mixedOneoffTaskqueue.poll()) != null) {
				int savedFor = next.getExecuteOnThread();
				if (savedFor == ParaTaskHelper.ANY_THREAD_TASK || savedFor == workerID) {
					if (next.executeAttempt()) {
						return next;
					} else {
						next.enqueueSlots(true);	//-- task is considered complete, so execute slots
					}
				} else {
					privateQueues.get(savedFor).add(next);
				}
			}
		}
		
		
		return null;
	}

	
	@Override
	protected void initialise() {
		mixedMultiTaskqueue = new FifoLifoQueue<TaskID<?>>();
		privateQueues = new ArrayList<AbstractQueue<TaskID<?>>>();
		mixedOneoffTaskqueue = new FifoLifoQueue<TaskID<?>>();
		initialiseWorkerThreads();
	}
}
