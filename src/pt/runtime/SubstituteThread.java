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

import java.util.concurrent.ExecutionException;

/*
 * 
 * This was only an initial concept idea, but was opted to not implement such an approach due to poor performance.
 * It is however still included for completeness, even though it's not used. 
 * 
 */
public class SubstituteThread extends TaskThread {
	
	private TaskID<?> taskIDWaitingFor;
	private int substituteForThreadID;
	
	// This constructor is used when want to create a substitute until a TaskThread wakes up again (blocked on non-ParaTask objects)
	public SubstituteThread(Taskpool taskpool, int substituteForThreadID) {
		this(null,taskpool, substituteForThreadID);
	}
	
	public SubstituteThread(TaskID<?> taskIDWaitingFor, Taskpool taskpool, int substituteForThreadID) {
		super(taskpool);
		this.taskIDWaitingFor = taskIDWaitingFor;
		this.substituteForThreadID = substituteForThreadID;
	}
	
	@Override
	public void run() {
		
		while (!taskIDWaitingFor.hasCompleted()) {
			
			// first check to see if taskIDWaitingFor is still waiting on other dependences

			boolean onWaitingQueue =  false;	// testing
//			boolean onWaitingQueue =  taskpool.onWaitingQueue(taskIDWaitingFor);
			
			// TODO   this is temporary to see if even need the substitute thread...
			TaskID<?> task = taskpool.workerPollNextTask();		// non-blocking, returns null if taskpool is empty
//			TaskID task = null;
			
			if (task == null) {
				// we didnt find a task from global queue... this means taskIDWaitingFor is either executing or on waitingQueue
				
				if (onWaitingQueue) {
					// must not retire yet.. sleep for a while and check again from the top
					try {
						Thread.sleep(1000);
//						Thread.sleep(TaskThread.THREAD_DELAY);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				} 
			} else {
				// substitute thread got a task, so execute it
				
				boolean success = executeTask(task);
				if (success) {
					Object result = null;
					try {
						result = task.getReturnResult();
					} catch (ExecutionException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					System.out.println("SUCCESS!! SubstituteThread " + threadID + "(" + Thread.currentThread().getId() +") executed task: " + task.globalID() + " of method: " 
							+ task.getTaskInfo().getMethod().getName() + "  with return value: " + result);
				} else {
					if (task.hasUserError()) {
						System.out.print(" --- This failure is user-error ( " + task.getException().getMessage() + " )  --- " );
					}
					System.out.println("FAILED!! SubstituteThread " + threadID + " ( " + Thread.currentThread().getId()+ ")"+ " wants to execute task: " + task.globalID() + " of method: " 
							+ task.getTaskInfo().getMethod().getName());
				}
				
				// inform any TaskIDs that were waiting on currentTask
				task.enqueueSlots(false);
			}
		}
		System.out.println("SubstituteThread " + threadID + " will end now since TaskID " + taskIDWaitingFor.globalID() + "  has completed");
		//taskIDWaitingFor.substituteThreadRetiring(substituteForThreadID);
	}
}
