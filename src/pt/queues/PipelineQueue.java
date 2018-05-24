package pt.queues;

import java.util.concurrent.LinkedBlockingDeque;

import pt.runtime.TaskID;

public class PipelineQueue<E> extends LinkedBlockingDeque<E> {
	
	private TaskID head;
	private TaskID tail;

	public PipelineQueue(TaskID head, TaskID tail) {
		this.head = head;
		this.tail = tail;
	}
	
	public TaskID getHeadTask() {
		return head;
	}
	
	public TaskID getTailTask() {
		return tail;
	}
	
	public void setHeadTask(TaskID tid) {
		head = tid;
	}
	
	public void setTailTask(TaskID tid) {
		tail = tid;
	}
}
