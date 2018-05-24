package pt.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 
 * @author Weng Hao
 * 
 * 	Work-First - Task Depth Control. Variation of the WorkStealing task scheduler.
 * 	While this implementiaton initially follows the behaviour of the WorkStealing task scheduler,
 * 	it is restricted to enqueuing tasks of a certain task depth.
 * 	The Task Depth Control is based on the depth of the task hierarchy. Each task will be assigned
 * 	a number which will correspond to the depth of a task in a task hierarchy, representing a node
 * 	in a tree-like structure.
 * 	If the depth of a task is found to have exceeded the taskDepthThreshold value, then the task
 * 	scheduler will no longer enqueue this task (and its future subtasks).
 * 	The intention of the Task Depth Control is to generate enough tasks for the processors for 
 * 	task distribution, while minimising the total amount of overhead generated from task enqueuing.
 * 	A task depth cut-off ensures that only a certain amount of tasks will be enqueued, while the
 * 	remaining tasks will be directly processed by the workers and reducing any unnecessary overhead
 * 	once tasks have been distributed to the various processors in the system.
 *
 */

public class TaskpoolLIFOWorkFirstTaskDepth extends TaskpoolLIFOWorkStealing {
	
	//Task Depth threshold
	private static int taskDepthThreshold = 8;
	
	/**
	 * 	@Override
	 * 	Creates a TaskID for the specified task (whose details are contained in the TaskInfo). It then returns the TaskID after 
	 * 	the task has been queued. If the depth of a task exceeds the threshold, tasks are no longer queued and are executed
	 * 	directly instead. 
	 * 	This method is generic and schedule-specific to Work-First. 
	 */
	public TaskID<?> enqueue(TaskInfo taskinfo) {
		
		TaskID taskID = new TaskID(taskinfo);
		
		ArrayList<TaskID<?>> allDependences = null;
		
		//-- determine if this task is being enqueued from within another task.. if so, set the enclosing task (needed to 
		//--		propogate exceptions to outer tasks (in case they have a suitable handler))
		Thread rt = taskinfo.setRegisteringThread();
		
		if (rt instanceof TaskThread) {
			TaskID<?> parentTask = ((TaskThread)rt).currentExecutingTask();
			taskID.setEnclosingTask(parentTask);
			taskID.setTaskDepth(parentTask.getTaskDepth()+1);
		}
		
		if(taskID.getTaskDepth() >= taskDepthThreshold) {
			/*
			 * 	Directly extracts the method of the task to operate on the class sequentially.
			 * 	Also while invoking the sequential method of the task, the return result has also been set.
			 */
			try {
				Method m = taskinfo.getMethod();
				taskID.setReturnResult(m.invoke(taskinfo.getInstance(), taskinfo.getParameters()));
				m = null;
				
				/*
				 * 	Once successfully invoked, clean up the rest of the TaskID info.
				 */
				taskID.setComplete();
				
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else { 

			if (taskinfo.getDependences() != null)
				allDependences = ParaTask.allTasksInList(taskinfo.getDependences());
		
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
		}
		
		return taskID;
	}
	
	
	/**
	 * 	Assigns the threshold value that is used to determine the
	 * 	number of tasks to be enqueued, before Work-First is enforced.
	 * 	In TaskpoolLIFOWorkFirstTaskDepth, the setThreshold() and
	 * 	getThreshold() refers to the level of depth that a task has
	 * 	within the task hierarchy.
	 * 	The default value for the taskDepthThreshold is 8.
	 * 	@param threshold
	 */
	public static void setThreshold(int threshold) {
		taskDepthThreshold = threshold;
	}
	
	/**
	 * 	Returns the current threshold value set for the given task
	 * 	scheduler.
	 * 	In TaskpoolLIFOWorkFirstTaskDepth, the getThreshold() refers
	 * 	to the level of depth that a task has within the task hierarchy.
	 * 	@return
	 */
	public static int getThreshold() {
		return taskDepthThreshold;
	}
}
