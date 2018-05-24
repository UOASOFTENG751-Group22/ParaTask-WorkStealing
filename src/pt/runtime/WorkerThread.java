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

public class WorkerThread extends TaskThread {
	
	/*
	 * When creating a new worker, indicate it is dedicated for multi task or dedicated for one-off task
	 * 
	 * Add a variable to check if the worker thread should go out of the loop.
	 * 
	 * Add a variable to indicate if the worker thread is waiting for tasks completion
	 * 
	 * Create a static variable used to tell if some threads need to be cancelled or not.
	 * 
	 * */
	private boolean isMultiTaskWorker;

	private boolean isCancelled = false;
	
	private boolean isCancelRequired = false; 
	
	public WorkerThread(int globalID, int localID, Taskpool taskpool, boolean isMultiTaskWorker) {
		super(taskpool, isMultiTaskWorker);
		
		if (threadID != globalID)
			throw new IllegalArgumentException("WorkerID does not match its globalID -- WorkerID:(" + threadID + "), globalID:(" + globalID + ")\n"
					+ " - should create WorkerThreads first");	
		
		if (isMultiTaskWorker) {
			if (threadLocalID != localID)
				throw new IllegalArgumentException("Multi-task worker LocalID does not match the localID -- WorkerID:(" + threadLocalID + "), localID:(" + localID +")\n"
						+ " - should create WorkerThreads first");
		}
		this.isMultiTaskWorker = isMultiTaskWorker;
	}
	
	/* 
	 * This method is called to tell the worker to execute ONE other task from the taskpool (if it finds one), 
	 * otherwise it will sleep for the specified time delay
	 * 
	 * returns true if it did execute another task.. otherwise false if it ended up sleeping instead
	 * 
	 * @author Kingsley
	 * @since 23/05/2013
	 */
	public boolean executeAnotherTaskOrSleep() {

		TaskID task = taskpool.workerPollNextTask();
		if (task != null) {
			executeTask(task);
			return true;
		} else {
			try {
				Thread.sleep(ParaTaskHelper.WORKER_SLEEP_DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	
	/*
	 * @author Kingsley
	 * @since 23/05/2013
	 * 
	 * If worker thread find a poison pill from within this "run()" method, which means
	 * it is not waiting for some other tasks finish first. 
	 * Set "isWaiting" to false, in order to examine this variable inside of the poison pill.
	 * 
	 * @since 25/05/2013
	 * Think it is un-necessary to distinguish nested and non-nested situation. Cancel "isWaiting".
	 * */
	@Override
	public void run() {
		while (true) {
			TaskID task = taskpool.workerTakeNextTask();
			boolean success = executeTask(task);
			
			if (isCancelRequired) {
				if (isCancelled) {
					break;
				}else {
					LottoBox.tryLuck();
					
					if (isCancelled) {
						break;
					}
				}
			}
		}
	}

	protected boolean isMultiTaskWorker() {
		return isMultiTaskWorker;
	}

	protected void setMultiTaskWorker(boolean isMultiTaskWorker) {
		this.isMultiTaskWorker = isMultiTaskWorker;
	}
	protected void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	protected boolean isCancelled() {
		return isCancelled;
	}

	protected void requireCancel(boolean cancelRequired){
		this.isCancelRequired = cancelRequired;
	}

	protected boolean isCancelRequired() {
		return isCancelRequired;
	}
}
