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
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Classes which implement this interface provide specifications regarding scheduling policies such as:<br>
 * 1- How to deal with tasks that are ready to be executed.<br>
 * 2- How to enqueue tasks (i.e. adding them to global or private task queues).<br>
 * 3- How to enqueue multi-task groups.<br>
 * 4- Different approaches for threads taking tasks from schedule-specific task pools/queues.<br>
 * 5- Managing different types of task pools/queues.<br>
 * 
 * @author Mostafa Mehrabi
 * @since 9/9/2014
 * */
public interface Taskpool {
		/**
	* The specified task is currently on the waiting queue since it has some dependences. However, all thoses dependences have 
	* now been met and the task is ready to be scheduled for execution. 
	* @param taskID
	*/
	public void nowReady(TaskID<?> taskID);
	
	/**
	* Returns the count of currently active interactive tasks. This is usually to know how many threads there are.
	* @return
	*/
	public int getActiveInteractiveTaskCount();
	
	/**
	* Used to decrement the count of interactive tasks
	* @param taskID	The task that has just completed
	*/
	public boolean interactiveTaskCompleted(TaskID<?> taskID);
	
	/**
	* Enqueues the specified task, whose information is contained in the TaskInfo. It then returns a TaskID
	* to represent that task.
	* @param taskinfo
	* @return
	*/
	public TaskID enqueue(TaskInfo taskinfo);
	
	/**
	* Enqueues the specified TaskInfo as a multi-task, creates "count" inner tasks and places them in a TaskIDGroup
	* which is then returned.
	* @param taskinfo
	* @param count
	* @return
	* 
	* 
	* */
	
	public TaskIDGroup enqueueMulti(TaskInfo taskinfo, int count);
	
	/**
	* The worker thread polls the task pool for a task.. If there isn't one, then it returns 
	* immediately (returns null in such a case).
	* @return
	*/
	public TaskID workerPollNextTask();	
	
	/**
	*	The worker thread blocks until it gets a task to execute.  
	* @return
	*/
	public TaskID workerTakeNextTask();
	
	public boolean executeSynchronously(int cutoff);
	
	public void printDebugInfo();
	
	public int totalNumTasksExecuted();
	
	
	/**
	 * 
	 * @Author : Kingsley
	 * @since : 02/05/2013 
	 * Used to access local one-off task queues by thread pool when initialization.
	 * 
	 * @since : 18/05/2013 
	 * Used to access private task queues by thread pool when initialization.
	 *  
	 * */
	public Map<Integer, LinkedBlockingDeque<TaskID<?>>> getLocalOneoffTaskQueues();
	
	public List<AbstractQueue<TaskID<?>>> getPrivateTaskQueues();
}
