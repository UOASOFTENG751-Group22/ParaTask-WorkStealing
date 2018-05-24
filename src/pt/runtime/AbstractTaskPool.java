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

import java.lang.ref.WeakReference;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import pt.queues.FifoLifoQueue;

public abstract class AbstractTaskPool implements Taskpool {
	
	protected final static int INITIAL_QUEUE_CAPACITY = 11;  
	
	protected final static Comparator<TaskID<?>> FIFO_TaskID_Comparator = new Comparator<TaskID<?>>() {
		@Override
		public int compare(TaskID<?> o1, TaskID<?> o2) {
			return o1.globalID - o2.globalID;
		}
	};
	
	protected final static Comparator<TaskID<?>> LIFO_TaskID_Comparator = new Comparator<TaskID<?>>() {
		@Override
		public int compare(TaskID<?> o1, TaskID<?> o2) {
			return o2.globalID - o1.globalID;
		}
	};
	
		
	protected ConcurrentHashMap<TaskID<?>, Object> waitingTasks = new ConcurrentHashMap<TaskID<?>, Object>();
	protected PriorityBlockingQueue<TaskID<?>> globalMultiTaskqueue = null;
	protected PriorityBlockingQueue<TaskID<?>> globalOne0ffTaskqueue = null;
	protected FifoLifoQueue<TaskID<?>> mixedMultiTaskqueue = null;
	protected FifoLifoQueue<TaskID<?>> mixedOneoffTaskqueue = null;
	protected List<AbstractQueue<TaskID<?>>> privateQueues;
	protected Map<Integer, LinkedBlockingDeque<TaskID<?>>> localOneoffTaskQueues = null;
	protected ThreadLocal<Integer> lastStolenFrom = null;	
	protected static final int NOT_STOLEN = -1;			
	private AtomicInteger interactiveTaskCount = new AtomicInteger(0);
	
	protected AbstractTaskPool() {
		initialise();
	}
	
	protected ConcurrentLinkedQueue<WeakReference<InteractiveThread>> cachedInteractiveThreadPool = new ConcurrentLinkedQueue<WeakReference<InteractiveThread>>();
	
	/*
	 * (schedule-specific) 
	 * The schedule-specific enqueuing of a ready task is defined here (not in the public enqueue() and enqueueMulti() methods, 
	 * since those are generic and will eventually use this method). 
	 * This method will not be called for interactive tasks, since the enqueue(), etc will check this beforehand.
	 * 
	 * This method is also not necessarily executed by the actual original enqueueing thread (since this might be called later since
	 * the task was waiting for other tasks to complete).
	 *  
	 */
	protected abstract void enqueueReadyTask(TaskID<?> taskID); 
	
	/*
	 * (schedule-specific) 
	 * The worker thread polls for a task to execute. If there currently isn't one, then it returns null.
	 */
	public abstract TaskID workerPollNextTask();	
	
	/*
	 * (schedule-specific)
	 * Performs initialisation specific to the schedule. 
	 */
	protected abstract void initialise();
	
	/*
	 * Creates a TaskID for the specified task (whose details are contained in the TaskInfo). It then returns the TaskID after 
	 * the task has been queued. This method is generic and not schedule-specific. 
	 */
	public TaskID<?> enqueue(TaskInfo taskinfo) {
		//before the first task is enqueued, scheduling types and thread sizes can change
		if(!ParaTask.paraTaskStarted())
			ParaTask.paraTaskStarted(true);
		
		ArrayList<TaskID<?>> allDependences = null;
		if (taskinfo.getDependences() != null)
			allDependences = ParaTask.allTasksInList(taskinfo.getDependences());
		
		TaskID<?> taskID = new TaskID(taskinfo);
		
		//determine if this task is being enqueued from within another task
		//needed to propagate exceptions to outer tasks (in case they have a suitable handler))
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
		
		return taskID;
	}
	
	public TaskIDGroup<?> enqueueMulti(TaskInfo taskinfo, int count){
		//before the first task is enqueued, scheduling types and thread sizes can change
		if(!ParaTask.paraTaskStarted())
			ParaTask.paraTaskStarted(true);
		
		if (count <= 0)
			count = ThreadPool.getMultiTaskThreadPoolSize();
		
		TaskIDGroup<?> group = new TaskIDGroup(count, taskinfo);
		group.setCount(count);
		
		ArrayList<TaskID<?>> allDependences = null;
		if (taskinfo.getDependences() != null)
			allDependences = ParaTask.allTasksInList(taskinfo.getDependences());
			
		Thread rt = taskinfo.setRegisteringThread();
		
		if (rt instanceof TaskThread)
			group.setEnclosingTask(((TaskThread)rt).currentExecutingTask());
		
		if (taskinfo.hasAnySlots())
			taskinfo.setTaskIDForSlotsAndHandlers(group);
		
		if (allDependences == null)
			if (group.isInteractive()) 
				startInteractiveTask(group);
			else
				enqueueReadyTask(group);
		else
			enqueueWaitingTask(group, allDependences);
		
		return group;
	}
	
	/*
	 * The worker thread blocks until it gets a task to execute.   
	 * 
	 * This method only returns when it finds an appropriate task for the calling worker (therefore appears as blocking).
	 * 
	 * If keeps polling for a task (this polling is schedule-specific). If it did not find anything from the poll, then it 
	 * sleeps before trying again, and again.
	 * 
	 */
	public TaskID<?> workerTakeNextTask() {
		while (true) {
			TaskID<?> next = workerPollNextTask();
			
			if (next != null) 
				return next;
			
			try {
				Thread.sleep(ParaTaskHelper.WORKER_SLEEP_DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * Returns the count of currently active interactive tasks. This is usually to know how many threads there are.
	 */
	public int getActiveInteractiveTaskCount() {
		return interactiveTaskCount.get();
	}
	
	/**
	 * Used to decrement the count of interactive tasks
	 */
	public boolean interactiveTaskCompleted(TaskID<?> taskID) {
		if (taskID != null && taskID.isInteractive()){
			interactiveTaskCount.decrementAndGet();
			return true;
		}
		return false;
	}
	
	protected void startInteractiveTask(TaskID<?> taskID) {
		if (!taskID.isInteractive() || taskID == null)
			return;
		
		else if (taskID instanceof TaskIDGroup<?>){
			TaskIDGroup<?> taskIDGroup = (TaskIDGroup<?>) taskID;
			int taskCount = taskIDGroup.groupSize();
			TaskInfo taskInfo = taskIDGroup.getTaskInfo();
			taskInfo.setSubTask(true);
			for (int taskIndex = 0; taskIndex < taskCount; taskIndex++){
				TaskID<?> subTaskID = new TaskID(taskInfo);
				subTaskID.setRelativeID(taskIndex);				
				subTaskID.setSubTask(true);
				subTaskID.setPartOfGroup(taskIDGroup);
				taskIDGroup.add(subTaskID);
				startInteractiveTask(subTaskID);
			}
			taskIDGroup.setExpanded(true);
		}
		
		else{
			interactiveTaskCount.incrementAndGet();
			for (WeakReference<InteractiveThread> interactiveRef : cachedInteractiveThreadPool){
				InteractiveThread interactiveThread = interactiveRef.get();
				if(interactiveThread.isInactive()){
					interactiveThread.setTaskID(taskID);
					return;
				}
			}
			
			InteractiveThread newInteractiveThread = new InteractiveThread(this, taskID);
			newInteractiveThread.start();
			cachedInteractiveThreadPool.add(new WeakReference<InteractiveThread>(newInteractiveThread));
		}
	}
	
	protected void startPipelineTask(TaskID<?> taskID) {
		PipelineThread pt = new PipelineThread(this, taskID);
		taskID.setPipelineThread(pt);
		pt.start();
	}
	
	/*
	 * There is just one waiting queue, therefore adding to the waiting queue is not schedule-specific.
	 */
	protected void enqueueWaitingTask(TaskID<?> taskID, ArrayList<TaskID<?>> allDependences) {

		if (allDependences.size() > 0) {
			waitingTasks.put(taskID, "");
			taskID.setRemainingDependences(allDependences);
			
			for (int d = 0; d < allDependences.size(); d++) {
				allDependences.get(d).addWaiter(taskID);
			}
		} else {
			enqueueReadyTask(taskID);
		}
	}
	

	/*
	 * Removes the specified task off the waiting queue and onto the ready-queue. 
	 */
	public void nowReady(TaskID<?> waiter) {
		/*
		 * remove 'waiter' from the waiting collection, and put it onto the ready queue
		 * ensures that it is only enqueued once (so that enqueuing it a second time will fail)
		 * */
		Object obj = waitingTasks.remove(waiter);
		if (obj != null) {
			if (waiter.isInteractive())
				startInteractiveTask(waiter);
			else
				enqueueReadyTask(waiter);
		}
	}
	
	protected void initialiseWorkerThreads() {
		ThreadPool.initialize(this);
	}
	
	public boolean executeSynchronously(int cutoff) {
		return false;
	}
	
	public void printDebugInfo() {
		System.out.println("Debug info for TaskPool...");
		
		System.out.println(" ----------------  currently all debug info removed ");
	}
	
	public int totalNumTasksExecuted() {
		int total = 0;
		return total;
	}

	public Map<Integer, LinkedBlockingDeque<TaskID<?>>> getLocalOneoffTaskQueues() {
		return localOneoffTaskQueues;
	}
	
	public List<AbstractQueue<TaskID<?>>> getPrivateTaskQueues() {
		return privateQueues;
	}
}
