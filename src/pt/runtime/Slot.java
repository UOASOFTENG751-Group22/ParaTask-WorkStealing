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
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 *This class allows the user to define a handler from one of the Functor types (functional interfaces)
 * which could be done by using lambda expressions as well. The user-defined handler will be associated
 * to the corresponding Functor instance in this class. Once the <code>execute</code> method of a <code>slot</code>
 * is called, the method executes the <code>execute</code> method of the corresponding handler which is 
 * an instance of one of the Fucntors. 
 * <br><br>
 * This class also allows storing and retrieving the intermediate results of a taskID.
 * 
 * @author Mostafa Mehrabi
 * @since  4/9/2014
 * 
 * */
public class Slot {
	public static enum SetCompleteSlot {TRUE, FALSE}
	
	final public static Slot quit = new Slot();
	
	private Method method;
	private Object instance;

	private ConcurrentLinkedQueue<Object> interResults = null;
	private Class interResultType = null;
	
	private SetCompleteSlot isASetCompleteSlot = SetCompleteSlot.FALSE;
	
	private TaskID<?> taskID = null; 	// TODO this is the task for which this slot is attached to (who should assign it?)
				// cannot be assigned at the time the slot is created (since the TaskID wasn't created just yet)
	
	private boolean isIntermediateResultSlot = false;
	
	private Slot() {
	}
	
	public Slot(Method method, Object instance, boolean isIntermediateResultSlot) {
		this.method = method;
		this.instance = instance;
		this.isIntermediateResultSlot = isIntermediateResultSlot;
	}

	public Slot(Method method, Object instance, boolean isIntermediateResultSlot, SetCompleteSlot isASetCompleteSlot) {
		this(method, instance, isIntermediateResultSlot);
		this.isASetCompleteSlot = isASetCompleteSlot;
	}
	
	public boolean isASetCompleteSlot() {
		return isASetCompleteSlot == SetCompleteSlot.TRUE;
	}
	
	public void addIntermediateResult(Class type, Object value) {
		if (interResults == null) {
			interResults = new ConcurrentLinkedQueue<Object>();
			interResultType = type;
		}
		interResults.add(value);
	}
	
	public Object getNextIntermediateResultValue() {
		return interResults.poll();
	}
	
	public Class getIntermediateResultType() {
		return interResultType;
	}
	
	public Method getMethod() {
		return method;
	}
	
	public Object getInstance() {
		return instance;
	}

	public TaskID<?> getTaskID() {
		return taskID;
	}

	public void setTaskID(TaskID<?> taskID) {
		this.taskID = taskID;
	}

	public boolean isIntermediateResultSlot() {
		return isIntermediateResultSlot;
	}
}
