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

import org.omg.CORBA.PUBLIC_MEMBER;


/**
* @author Mostafa Mehrabi
* @author Nasser Giacaman
* @author Oliver Sinnen
* 
* <br><br>
* Helper methods for the ParaTask runtime. This class contains various functions to set up the ParaTask runtime. 
* Importantly the type of scheduling, thread pool type, thread pool size, the EDT and the task listener are set 
* and retrieved via this class. 
* This class is also able to receive a list of GrouptaskIDs, flatten them and return the flattened list.
* 
* <br><br>
* All applications making use of the ParaTask features should invoke {@link ParaTask#init()} early in the <code>main</code>
* method. This will initialise various aspects of the ParaTask runtime.
**/
public class ParaTask {
	
	/**
	 * 
	 * @Author : Kingsley
	 * @since : 29/04/2013
	 * 
	 * ParaTask does not need to know the thread pool size. It should ask thread pool to get
	 * the size of the pool. 
	 * 
	 * User does not need to know anything about thread, then it might be a good idea to set
	 * the thread pool size through the ParaTask rather than accessing the thread pool directly.
	 * 
	 * */
	
	//private static int threadPoolSize = Runtime.getRuntime().availableProcessors();
	private static ScheduleType scheduleType = ScheduleType.MixedSchedule;
	private static boolean isInitialized = false;
	private static boolean paraTaskStartedWorking = false;


	private static Thread EDT = null;		// a reference to the EDT
	private static AbstractTaskListener listener;	// the EDT task listener
	
	static long WORKER_SLEEP_DELAY = 200;
	static long INTERACTIVE_SLEEP_DELAY = 60000;
	
		
	ParaTask(){
		
	}
	
	public static Thread getEDT() {
		return EDT;
	}
	
	/**
	 * 
	 * Enum representing the possible schedules that ParaTask supports.
	 * 
	 * @author Nasser Giacaman
	 * @author Oliver Sinnen
	 */
	public static enum ScheduleType { 
		/**
		 * Tasks are queued to a shared global queue and are executed using a first in first out policy. 
		 */
		WorkSharing, 
		
		/**
		 * Tasks are queued to the queue local to the enqueing thread (or to a random thread's queue if the
		 * enqueing thread is not a worker thread). Tasks are executed using a last in first out policy when 
		 * taken from the thread's own queue. Otherwise, tasks are stolen from another thread's queue using
		 * a first in first out policy.
		 */
		WorkStealing,
		
		/**
		 * A combination of work-stealing and work-sharing. When a task is enqueued by a worker thread, this 
		 * behaves as work-stealing. When a task is enqueued by a non-worker thread (e.g. main thread or event 
		 * dispatch thread), this behaves as work-sharing. A worker thread always favours to execute tasks 
		 * from its local queue before helping with the global shared queue.    
		 */
		MixedSchedule,
		
		/**
		 * 	New: WorkFirst - Task Depth Control. A variation of WorkStealing. When the depth level 	of a task 
		 * 	has been found to exceed the task depth threshold, the task will not be enqueued and will be 
		 * 	processed directly instead.
		 */
		WorkFirstTaskDepth,
		
		/**
		 * 	New: WorkFirst - Global Task Population Control. A variation of WorkStealing. Restricts the number
		 * 	of tasks enqueued in the task scheduler at any given time. If the number of tasks enqueued in the
		 * 	system exceeds the given threshold, then the task scheduler will stop enqueuing and will process
		 * 	tasks directly instead.
		 */
		WorkFirstGlobal,
		
		/**
		 * 	New: WorkFirst - Local Task Queue Control. A variation of WorkStealing. Restricts the number of
		 * 	tasks that can be enqueued in each individual local one-off task queue. If the number of tasks in
		 * 	a local one-off task queue exceeds the given threshold, then the affected task queue will no longer
		 * 	enqueue further tasks and will directly process the task instead.
		 */
		WorkFirstLocal
};
		
		
   /**
	* 
	* Enum representing the possible thread pool types that ParaTask supports.
	* 
	* @author Kingsley
 	* @since 27/05/2013
  	*/
	public static enum ThreadPoolType{
	    	ALL, ONEOFF, MULTI
	 }	
		
		
	static void paraTaskStarted(boolean started){
		ParaTask.paraTaskStartedWorking = started;
	}
	
	static boolean paraTaskStarted(){
		return ParaTask.paraTaskStartedWorking;
	}
	
	static boolean isInitialized() {
		return isInitialized;
	}

    /**
     * Set the size of the thread pool. To have any effect, this must be executed very early before 
     * ParaTask creates the runtime. 
     * @param size
     */
    public static boolean setThreadPoolSize(ThreadPoolType threadPoolType, int size) {
    	if (size < 1)
			throw new IllegalArgumentException("Trying to create a Taskpool with " + size + " threads");
		
    	if(paraTaskStarted())
    		return false;
    	
    	ThreadPool.setPoolSize(threadPoolType,size);
    	return true;
    }
    
  
    /**
     * Set the scheduling scheme. This only has an effect if no tasks have been executed yet 
     * (i.e. must be called before ParaTask is initialized). 
     * This method returns <code>false</code> if it fails to adjust thread pool. This
     * happens when ParaTask has already started working (i.e., TaskInfos are enqueued and TaskIDs are
     * created), otherwise it returns <code>true</code>
     * 
     * @param type The schedule to use.
     * @return boolean <code>true</code> if scheduling type is changed successfully, otherwise <code>false</code>.
     */
    public static boolean setScheduling(ScheduleType type) {
    	if (ParaTask.paraTaskStarted())
    		return false;
    	scheduleType = type;
    	return init(scheduleType);
    }
    
    /**
     * Returns the schedule being used in the runtime.  
     * @return		The schedule being used.
     */
    public static ScheduleType getScheduleType() {
    	return scheduleType;
    }
    
    /**
     * Returns the size of the thread pool.
     * 
     * @return	The thread pool size.
     */
    public static int getThreadPoolSize(ThreadPoolType threadPoolType) {
    	if(!isInitialized())
    		init();
    	return ThreadPool.getPoolSize(threadPoolType);
    }
    
    public static int getActiveCount(ThreadPoolType threadPoolType){
    	return ThreadPool.getActiveCount(threadPoolType);
    }
	
	/**
	 * Returns a count of the number of active interactive tasks. Useful if need to decide whether a task 
	 * should be invoked interactively or not (e.g. to limit thread count). 
	 * @return	The number of active interactive tasks.
	 */
	public static int activeInteractiveTaskCount() {
		return TaskpoolFactory.getTaskpool().getActiveInteractiveTaskCount();
	}
	
	
	public static boolean init(){
		if(scheduleType == null)
			scheduleType = ScheduleType.MixedSchedule;
		return init(scheduleType);
	}
	
	/*
	 * To be executed by the main thread (i.e. inside the <code>main</code> method). Registers the main thread and event 
	 * dispatch thread with ParaTask, as well as other ParaTask runtime settings.
	 */
	private static boolean init(ScheduleType scheduleType) {
		if(paraTaskStarted())
			return false;
		
		isInitialized = false;
		while(!isInitialized){
			GuiThread.init();
		
			try {
				ParaTaskHelper.setCompleteSlot = 
					ParaTaskHelper.class.getDeclaredMethod("setComplete", new Class[] {TaskID.class});
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}

			ThreadPool.resetThreadPool();
			TaskpoolFactory.resetTaskPool();
			
			//-- Create the task pool
			TaskpoolFactory.getTaskpool();
			
			//-- initialize the EDT
			EDT = GuiThread.getEventDispatchThread();
			listener = new GuiEdtTaskListener();
			isInitialized = true;
			System.out.println("ParaTask.init EDT id: " + EDT.getId() + " EDT name: " + EDT.getName());
		}
		return true;
	}
	
	static AbstractTaskListener getEDTTaskListener() {
		if (EDT == null) {
			throw new RuntimeException("Please call ParaTask.init() early in the main method of your application!");
		}
		return listener;
	}	
	
	/**
	 * Flattens a list of TaskIDs. Only has an effect if some of the TaskIDs were actually TaskIDGroups.
	 * @param list	Input list of TaskIDs (with potentially some TaskIDGroups)
	 * @return	A list containing only TaskIDs (i.e. expanding the TaskIDGroups)
	 * @see #allTasksInGroup(TaskIDGroup)
	 * 
	 * @author Kingsley
	 * @date 2014/04/08
	 * When add dependency to a task(A), if the dependency is a TaskIDGroup, meaning that it is a Multi-Task,
	 * the task(A) should directly depends on this Multi-Task, rather than all its sub-tasks.
	 * 
	 * This is because of the scheme of Late-expansion. At this point, Multi-Task may have not been executed yet,
	 * then all its sub-tasks have not been created yet. 
	 * 
	 */
	public static ArrayList<TaskID<?>> allTasksInList(ArrayList<TaskID> list) {
		ArrayList<TaskID<?>> result = new ArrayList<TaskID<?>>();
		
		Iterator<TaskID> it = list.iterator();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}
	
	/**
	 * A recursive convenience function that digs into the TaskIDGroup and returns all the individual TaskIDs.
	 * @see #allTasksInList(ArrayList) 
	 * @return the TaskIDs inside <code>group</code> placed inside a new ArrayList
	 * */
	public static ArrayList<TaskID> allTasksInGroup(TaskIDGroup group) {
		ArrayList<TaskID> list = new ArrayList<TaskID>();
		 
		Iterator<TaskID> it = group.groupMembers();
		while (it.hasNext()) {
			TaskID id = it.next();
			if (id instanceof TaskIDGroup) {
				list.addAll(allTasksInGroup((TaskIDGroup)id));
			} else {
				list.add(id);
			}
		}
		return list;
	}

	

	/**
	 * 	Sets the threshold value for Work-First. When the threshold value has been exceeded,
	 * 	tasks will no longer be enqueued and will be processed directly by the workers
	 * 	instead.
	 * 	In WorkFirstTaskDepth, the setThreshold() refers to the task depth threshold.
	 * 	In WorkFirstGlobal, the setThreshold() refers to the upper bound threshold value.
	 * 	The secondary method, setLowerBoundThreshold() refers to the lower bound threshold value
	 * 	which will be described in setLowerBoundThreshold().
	 * 	In WorkFirstLocal, the setThreshold() refers to the number of tasks permitted in each
	 * 	local one-off task queue, before Work-First is enforced.
	 * 	@param threshold
	 *  @warning Code has not been thoroughly tested yet.
	 */
	public static void setThreshold(int threshold) {
		switch (ParaTask.getScheduleType()) {
		case WorkFirstTaskDepth:
			TaskpoolLIFOWorkFirstTaskDepth.setThreshold(threshold);
			break;
		case WorkFirstGlobal:
			TaskpoolLIFOWorkFirstGlobal.setThreshold(threshold);
			break;
		case WorkFirstLocal:
			TaskpoolLIFOWorkFirstLocal.setThreshold(threshold);
			break;
		}
	}
	
	/**
	 * 	Only applicable to the WorkFirstGlobal task scheduler.
	 * 	Sets the lower bound threshold value for the Work-First Global Task Population Control.
	 * 	While the upper bound threshold is used to halt enqueuing for the task scheduler, the
	 * 	lower bound threshold determines when the task scheduler is allowed to resume enqueuing
	 *  in the system again.
	 *  @warning Code has not been thoroughly tested yet.
	 */
	public static void setLowerBoundThreshold(int threshold) {
		switch (ParaTask.getScheduleType()) {
		case WorkFirstGlobal:
			TaskpoolLIFOWorkFirstGlobal.setLowerBoundThreshold(threshold);
			break;
		}
	}
	
	/**
	 *	Returns the threshold value set for the Work-First task scheduler.
	 *	For the WorkFirstGlobal, this value refers to the upper bound threshold value
	 *	which is used to restrict the number of enqueued tasks in the task scheduler.
	 * 	@return
	 *  @warning Code has not been thoroughly tested yet.
	 */
	public static int getThreshold() {
		switch (ParaTask.getScheduleType()) {
		case WorkFirstTaskDepth:
			return TaskpoolLIFOWorkFirstTaskDepth.getThreshold();
		case WorkFirstGlobal:
			return TaskpoolLIFOWorkFirstGlobal.getThreshold();
		case WorkFirstLocal:
			return TaskpoolLIFOWorkFirstLocal.getThreshold();
		default:
			break;
		}
		return 0;
	}
	
	/**
	 * 	Only applicable to the WorkFirstGlobal task scheduler.
	 * 	Returns the lower bound threshold value for the Global Task Population Control.
	 *  @warning Code has not been thoroughly tested yet.
	 */
	public static int getLowerBoundThreshold() {
		switch (ParaTask.getScheduleType()) {
		case WorkFirstGlobal:
			return TaskpoolLIFOWorkFirstGlobal.getLowerBoundThreshold();
		default:
			break;
		}
		return 0;
	}
	
}
