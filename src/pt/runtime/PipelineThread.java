/*
 *  Copyright (C) 2010 Nasser Giacaman, Oliver Sinnen, Jonathan Chow
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import pt.queues.PipelineQueue;

public class PipelineThread extends TaskThread {

	private TaskID task = null;
	private int elementsLeft = -1;
	
	private volatile boolean cancelRequested = false;

	public PipelineThread(Taskpool taskpool, TaskID task) {
		super(taskpool);
		this.task = task;
	}
	
	public void cancel() {
		cancelRequested = true;
	}

	@Override
	public void run() {

		TaskInfo info = task.getTaskInfo();
		Method method = info.getMethod();
		Object instance = info.getInstance();
		Object[] args = info.getParameters();
		Object result = null;

		// for debugging purposes
		String name = method.getName().replace("__pt__", "");
		this.setName(method.getName());

		int[] taskIdArgIndexes = info.getTaskIdArgIndexes();
		int[] queueArgIndexes = info.getQueueArgIndexes();

		// some taskIDs may also be queues
		List<Integer> combinedQueueArgIndexesList = new ArrayList<Integer>();
		for (int index : queueArgIndexes) {
			combinedQueueArgIndexesList.add(index);
		}

		// retrieve results from implicit taskids
		// these only need to be retrieved once
		for (int index : taskIdArgIndexes) {

			TaskID taskId = (TaskID) args[index];

			// if the dependent task is not a pipeline, block until result
			if (!taskId.isPipeline()) {
				try {
					args[index] = ((TaskID) args[index]).getReturnResult();
				} catch (InterruptedException e) {
					// TODO: what happens?
				} catch (ExecutionException e) {
					// TODO: what happens?
				}
			}

			// otherwise we replace taskid parameter with its output queue
			else {
				args[index] = taskId.getOutputQueue(task);
				combinedQueueArgIndexesList.add(index);
			}
		}

		// we need to take a copy of the args to preserve:
		// * references to queues that were passed as parameters
		// * indexing for getting to the queues
		Object[] argsCopy = Arrays.copyOf(args, args.length);

		// unpack list into array for more efficient handling
		int[] combinedQueueArgIndexes = new int[combinedQueueArgIndexesList.size()];
		for (int i = 0; i < combinedQueueArgIndexes.length; i++)
			combinedQueueArgIndexes[i] = combinedQueueArgIndexesList.get(i);

		// loop
		while (elementsLeft != 0) {
			//System.err.println(name + ": left=" + elementsLeft);
			boolean proceedWithCancel = false;
			
			// get objects from input queues
			int currentListIndex = 0;
			while (currentListIndex < combinedQueueArgIndexesList.size()) {
				int argIndex;
				
				// check if we should stop this thread
				if (cancelRequested) {
					//System.err.println(name + ": interrupted");
					
					// push all retrieved items back onto the queue
					for (int i = 0; i < currentListIndex; i++) {
						argIndex = combinedQueueArgIndexesList.get(i);
						
						PipelineQueue pqueue = (PipelineQueue)argsCopy[argIndex];
						pqueue.addFirst(args[argIndex]);
					}
					
					// stop retrieving items from the queues
					proceedWithCancel = true;
					break;
				}
				
				// get the next queue to take from
				argIndex = combinedQueueArgIndexesList.get(currentListIndex);
				BlockingQueue queue = (BlockingQueue)argsCopy[argIndex];
				try {
					args[argIndex] = queue.poll(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					// theoretically can't happen
					e.printStackTrace();
				}
				
				// check if we got something
				if (args[argIndex] != null)
					currentListIndex++;
			}
			
			// check flag to see if we are doing the cancel routine
			if (proceedWithCancel) {
				//System.err.println(name + ": cancelling");
				
				// check which parent queues are cancelled
				// use cancelled queues to determine 
				int minElements = -1;
				int maxElements = 0;
				for (int i : combinedQueueArgIndexesList) {
					BlockingQueue queue = (BlockingQueue)argsCopy[i];
					int queueSize = queue.size();

					if (queueSize > maxElements) 
						maxElements = queueSize;
					
					// see if this is an internal queue
					if (queue instanceof PipelineQueue) {
						PipelineQueue pQueue = (PipelineQueue)queue;
						
						if ((pQueue.getHeadTask() == null || pQueue.getHeadTask().cancelled)
								&& (minElements < 0 || queueSize < minElements)) {
							minElements = queueSize;
							
							// unregister the queue with the parent task so it gets no more updates
							if (pQueue.getHeadTask() != null)
								pQueue.getHeadTask().unregisterOutputQueue(pQueue);
						}
					}
				}
				
				// at this point, if minElements == -1 then no parent task was cancelled
				// in other words, this is the first task to be cancelled
				if (minElements >= 0) {
					elementsLeft = minElements;
				} else {
					elementsLeft = maxElements;
				}
				
				// reset flags
				proceedWithCancel = false;
				cancelRequested = false;
				
			} else {
				// execute the method
				try {
					result = method.invoke(instance, args);
				} catch (IllegalAccessException e) {
					// theoretically can't happen
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// theoretically can't happen
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO: handle exceptions from invoked method
					e.printStackTrace();
				}
				
				// write result to output queues
				task.writeToOutputQueues(result);
				
				// decrement elementsLeft if it is positive
				// only decrement if we didn't do any interrupt processing
				if (elementsLeft > 0)
					elementsLeft -= 1;
			}
		}
		
		// finished! send cancel to child tasks
		task.cancelChildTasks();
		
		//System.err.println(name + ": stopped");
	}
}
