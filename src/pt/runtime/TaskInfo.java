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
 
/**
 * 
 * Used to store certain information in order to invoke the task.. (i.e. before invoking the task.. therefore 
 * information after the task has been invoked (eg return value) are stored in the TaskID
 *
 */ 
public class TaskInfo {
	
	private Method method = null;
	private Object[] parameters = new Object[]{};
	private Object instance = null;
	private Thread registeringThread = null;
	private ArrayList<Slot> slotsToNotify = null;
	private ArrayList<Slot> interSlotsToNotify = null;
	private ArrayList<TaskID> dependences = null;
	
	// for implicit results/dequeuing
	private int[] taskIdArgIndexes = new int[]{};
	private int[] queueArgIndexes = new int[]{};
	
	private boolean hasAnySlots = false;
	private boolean isPipeline = false;
	
	//-- Should always ensure that the registered exceptions are kept lined up with the handlers 
	private ArrayList<Class> excHandler_registeredExceptions = null;
	private ArrayList<Slot> excHandler_handlers = null;
	
	private boolean isInteractive = false;
	
	/**
	 * 
	 * @author Kingsley
	 * @since 10/05/2013
	 * 
	 * Used to identify if it is a sub task for a multi task
	 * 
	 * */
	private boolean isSubTask = false;

	public boolean hasAnySlots() {
		return hasAnySlots;
	}
	
	/**
	 * Adds a dependency on the given task. If it is part of a pipeline,
	 * automatically make this task also a pipeline stage.
	 * @param otherTask
	 */
	public void addDependsOn(TaskID otherTask) {
		if (otherTask != null) {
			if (dependences == null)
				dependences = new ArrayList<TaskID>();
			
			dependences.add(otherTask);
			
			// mark as pipeline if a dependency is a pipeline stage
			if (otherTask.isPipeline())
				isPipeline = true;
		} else {
			System.err.println("ParaTask warning: TaskInfo.addDependsOn(): null dependence ignored by TASK");
		}
	}
	
	public boolean isPipeline() {
		return isPipeline;
	}
	
	public Object[] getParameters() {
		return parameters;
	}
	
	public int[] getTaskIdArgIndexes() {
		return taskIdArgIndexes;
	}
	
	public int[] getQueueArgIndexes() {
		return queueArgIndexes;
	}
	
	public Object getInstance() {
		return instance;
	}
	
	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}
	
	public void setIsPipeline(boolean isPipeline) {
		this.isPipeline = isPipeline;
	}
	
	public void setParameters(Object... parameters) {
		this.parameters = parameters;
	}
	
	public void setTaskIdArgIndexes(int... indexes) {
		this.taskIdArgIndexes = indexes;
	}
	
	public void setQueueArgIndexes(int... indexes) {
		this.queueArgIndexes = indexes;
	}

	public void setInstance(Object instance) {
		this.instance = instance;
	}
	
	public Thread getRegisteringThread() {
		return registeringThread;
	}
	
	public Thread setRegisteringThread() {
	try {
		if (GuiThread.isEventDispatchThread())
			registeringThread = ParaTask.getEDT();
		else
			
				registeringThread = Thread.currentThread();
	} catch (Exception e) {
		registeringThread = Thread.currentThread();
	}
		return registeringThread;
	}
	
	public void setTaskIDForSlotsAndHandlers(TaskID taskID) {
		if (slotsToNotify != null) {
			for (Iterator<Slot> it = slotsToNotify.iterator(); it.hasNext(); ) {
				it.next().setTaskID(taskID);
			}
		}
		if (interSlotsToNotify != null) {
			for (Iterator<Slot> it = interSlotsToNotify.iterator(); it.hasNext(); ) {
				it.next().setTaskID(taskID);
			}
		}
		if (excHandler_handlers != null) {
			for (Iterator<Slot> it = excHandler_handlers.iterator(); it.hasNext(); ) {
				it.next().setTaskID(taskID);
			}
		}
	}

	/**
	 * This method should be used in the order that the exception handlers are to be considered later on
	 * @param exceptionClass
	 * @param handler
	 */
	public void addExceptionHandler(Class exceptionClass, Slot handler) {
		if (excHandler_registeredExceptions == null)
			excHandler_registeredExceptions = new ArrayList<Class>();
		if (excHandler_handlers == null)
			excHandler_handlers = new ArrayList<Slot>();
		
		excHandler_registeredExceptions.add(exceptionClass);
		excHandler_handlers.add(handler);
		hasAnySlots = true;
	}
	
	
	/*
	 *	This method returns the first suitable handler (if any is found) for the specified exception. It considers the 
	 *	correct inheritance structure, and the order of handlers considered is the same order as the programmer listed in the trycatch  
	 */
	public Slot getExceptionHandler(Class occuredException) {
		if (excHandler_registeredExceptions == null)
			return null;
		for (int i = 0; i < excHandler_registeredExceptions.size(); i++) {
			if (ParaTaskHelper.isSubClassOf(occuredException, excHandler_registeredExceptions.get(i))) 
				return excHandler_handlers.get(i);
		}
		return null;
	}
	
	public void addInterSlotToNotify(Slot slot) {
		if (interSlotsToNotify == null)
			interSlotsToNotify = new ArrayList<Slot>();
		interSlotsToNotify.add(slot);
	}
	
	public void addSlotToNotify(Slot slot) {
		if (slotsToNotify == null)
			slotsToNotify = new ArrayList<Slot>();
		slotsToNotify.add(slot);
		hasAnySlots = true;
	}

	public ArrayList<Slot> getInterSlotsToNotify() {
		return interSlotsToNotify;
	}
	public ArrayList<Slot> getSlotsToNotify() {
		return slotsToNotify;
	}
	public ArrayList<TaskID> getDependences() {
		return dependences;
	}

	public boolean isInteractive() {
		return isInteractive;
	}

	public void setInteractive(boolean isInteractive) {
		this.isInteractive = isInteractive;
	}

	public boolean hasRegisteredHandlers() {
		return !excHandler_registeredExceptions.isEmpty();
	}

	/**
	 * 
	 * @author Kingsley
	 * @since 10/05/2013
	 * 
	 * Getter and Setter for isSubTask
	 * 
	 * */
	protected boolean isSubTask() {
		return isSubTask;
	}

	protected void setSubTask(boolean isSubTask) {
		this.isSubTask = isSubTask;
	}
}
