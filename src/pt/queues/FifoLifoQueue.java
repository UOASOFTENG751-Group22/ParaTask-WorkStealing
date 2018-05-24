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

package pt.queues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A mixed-schedule queue. Elements may be added using either a FIFO policy ({@link #addGlobal(Object)}), 
 * or a LIFO policy ({@link #addLocal(Object)}).
 * <br><br>
 * If only {@link #addLocal(Object)} is used, then this collection will essentially behave like a {@link WorkStealingQueue}.
 * <br><br>
 * If only {@link #addGlobal(Object)} is used, then this collection will essentially behave like a standard FIFO queue.  
 *	<br><br>
 * Having the ability to add both locally and globally allows for increased flexibility. For example, ParaTask uses this
 * queue to ensure fairness for non-recursive tasks, while also ensuring that recursive tasks are handled.      
 *	<br><br>
 * The "head of queue" in the context of a FifoLifoQueue refers to the element according to this schedule. There are three
 * possibilities where the "head of queue" element might come from. First, an 
 * element is attempted to be taken from the thread's local queue. If this fails, then an element is removed from the 
 * shared global queue. If this fails, an element is stolen from another thread's local queue.
 * 
 * @author Nasser Giacaman
 * @author Oliver Sinnen
 * 
 * @param <E> The type of elements held in this collection
 *
 */
public class FifoLifoQueue<E> extends WorkStealingQueue<E> {
	
	private LinkedBlockingQueue<E> globalQueue = new LinkedBlockingQueue<E>();
	
	private long[] threadPoolIDs = null;
	
	/**
	 * Creates an empty queue with maximum capacity and chunksize of 1.
	 */
	public FifoLifoQueue() {
		super();
	}
	
	/**
	 * Creates an empty queue with the specified chunksize and capacity.
	 * @param capacity	
	 * @param chunksize
	 */
	public FifoLifoQueue(int capacity, int chunksize) {
		super(capacity, chunksize);
	}
	
	/**
	 * Creates a queue containing the specified collection with the specified chunksize. Capacity is unlimited.
	 * @param c
	 * @param chunksize
	 */
	public FifoLifoQueue(Collection<? extends E> c, int chunksize) {
		super(c, chunksize);
	}
	
	/**
	 * Create an empty queue with unlimited capacity and chunksize of 1.
	 * If planning to use this collection for a thread-pool implementation where a special set of threads are 
	 * to be considered as worker-threads, then specify them here, or using setThreadPoolIDs() if using another
	 * constructor. In this case, those "special" threads will have their elements added locally when using add().
	 * 
	 * @param threadPoolIDs
	 * @see #setThreadPoolIDs(long[])
	 */
	public FifoLifoQueue(long[] threadPoolIDs) {
		this();
		this.threadPoolIDs = threadPoolIDs;
	}

	/**
	 * Set the set of threads that will be the worker-threads. When elements are added using add(E), those
	 * elements will be added locally. 
	 * @param threadPoolIDs
	 */
	public void setThreadPoolIDs(long[] threadPoolIDs) {
		this.threadPoolIDs = threadPoolIDs;
	}

	/**
	 * Add the specified element locally. The element will be executed using a work-stealing LIFO schedule.
	 * @param e		the element to add
	 * @return		<code>true</code> (as specified by <code>Collection.add(E)</code>) 
	 */
	public boolean addLocal(E e) {
		return super.add(e);	//-- Work-stealing add() 
	}
	
	/**
	 * Add the specified element globally. The element will be executed using a work-sharing FIFO schedule.
	 * @param e		the element to add
	 * @return		<code>true</code> (as specified by <code>Collection.add(E)</code>) 
	 */
	public boolean addGlobal(E e) {
		if (remainingCapacity.get() <= 0)
			throw new IllegalStateException("Cannot add element, collection capacity exceeded.");
		remainingCapacity.decrementAndGet();
		return globalQueue.add(e);
	}
	
	/*
	 * checks whether the specified id is a registered worker thread (registered by the user)
	 */
	private boolean isWorkerThread(long id) {
		if (threadPoolIDs == null)
			return false;
		
		for (int i = 0; i < threadPoolIDs.length; i++) {
			if (threadPoolIDs[i] == id)
				return true;
		}
		return false;
	}
	
	/**
	 * @deprecated		If the calling thread already has a local queue, or the calling thread is registered as a 
	 * 					worker thread, then this call is equivalent to {@link #addLocal(Object)}. Otherwise,
	 * 					the call is equivalent to {@link #addGlobal(Object)}. It is therefore recommended to explicitly use 
	 * 					{@link #addLocal(Object)} or {@link #addGlobal(Object)} to ensure the correct intention is performed.
	 */
	@Override
	public boolean add(E e) {
		
		long id = Thread.currentThread().getId();
		LinkedBlockingDeque<E> local = localDeques.get(id);
		
		if (local != null) {
			return addLocal(e);
		} else if (isWorkerThread(id)) {
			return addLocal(e);
		} else {
			return addGlobal(e);
		}
	}
	
	@Override
	public boolean contains(Object o) {
		if (globalQueue.contains(o))
			return true;
		return super.contains(o);
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == this)
			throw new IllegalArgumentException("Cannot drain to itself.");

		int amountDrained = globalQueue.drainTo(c, maxElements);
		remainingCapacity.getAndAdd(amountDrained);
		
		if (amountDrained < maxElements) {
			if (maxElements == Integer.MAX_VALUE)
				amountDrained += super.drainTo(c, Integer.MAX_VALUE);
			else
				amountDrained += super.drainTo(c, maxElements-amountDrained);
			//-- ramainingCapacity is modified in super class method if necessary		
			return amountDrained;
		} else {
			return amountDrained;
		}
	}
	
	@Override
	public boolean remove(Object o) {
		if (globalQueue.remove(o)) {
			remainingCapacity.incrementAndGet();
			return true;
		}
		return super.remove(o);
	}
	
	@Override
	public E element() {
		long id = Thread.currentThread().getId();
		LinkedBlockingDeque<E> deque = localDeques.get(id);
		
		E e;
		
		//-- try to get an element from the local deque
		if (deque != null) {
			e = deque.peekFirst();
			if (e != null)
				return e;
		}
		
		//-- try to get an element from the global queue
		e = globalQueue.peek();
		if (e != null)
			return e;
		
		//-- try to steal from the other threads
		Iterator<LinkedBlockingDeque<E>> otherDeques = localDeques.values().iterator();
		while (otherDeques.hasNext()){
			LinkedBlockingDeque<E> q = otherDeques.next();
			e = q.peekLast();
			if (e != null)
				return e;
		}
		
		//-- found nothing
		throw new NoSuchElementException("No element found in collection.");
	}

	@Override
	public E poll() {
		
		//-- check own local queue first
		E e = pollLocalQueue();
		if (e != null)
			return e;
		
		//-- try to get an element from the global queue
		e = globalQueue.poll();
		if (e != null) {
			
			if (chunksize != 1) {
				
				LinkedBlockingDeque<E> thiefDeque = localDeques.get(Thread.currentThread().getId());
				//-- if managed to steal one element, try to steal chunksize-1 more (in total stealing chunksize elements)
				int maxElements = chunksize-1;
				
				if (chunksize == HALF) {
					maxElements = globalQueue.size()/2;
				}

				globalQueue.drainTo(thiefDeque, maxElements);
			}
			
			remainingCapacity.incrementAndGet();
			return e;
		}
		
		//-- try to steal from the other threads
		e = attemptToStealRandom();
		if (e != null)
			return e;
		
		//-- found nothing
		return null;
	}

	@Override
	public void clear() {
		globalQueue.clear();
		super.clear();
	}
	
	@Override
	public boolean isEmpty() {
		if (!globalQueue.isEmpty())
			return false;
		return super.isEmpty();
	}
	
	@Override
	public int size() {
		int size = globalQueue.size();
		size += super.size();
		return size;
	}
	
	@Override
	protected ArrayList<E> asList() {
		ArrayList<E> list = new ArrayList<E>();
		list.addAll(globalQueue);
		list.addAll(super.asList());
		return list;
	}
}
