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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of a work-stealing queue. Elements are added and removed from the queue using a work-stealing policy.
 * <br><br>
 * Elements are added to a thread's local queue, and removed from
 * a thread's local queue using a last in first out (LIFO) policy. If no elements exist in the thread's local queue, 
 * an element is stolen using a first in first out (FIFO) policy  from another thread's local queue. 
 * <br><br>
 * Consequently, the "head of the queue" in the context of the WorkStealingQueue refers to the 
 * element according to this work-stealing schedule. In other words, "head of the queue" for the 
 * thread when it takes from its own local queue refers to the same end where elements are added (i.e. the LIFO end). 
 * Similarly, the "head of the queue" for a stealing thread refers to the opposite end of the
 * of the victim's queue (i.e. the FIFO end).
 * 
 * @author Nasser Giacaman
 * @author Oliver Sinnen
 *
 * @param <E> The type of elements held in this collection
 */
public class WorkStealingQueue<E> implements BlockingQueue<E> {
	
	protected static final int HALF = -1;
	
	protected final static int sleep_amount_milli = 5;
	
	protected ConcurrentHashMap<Long, LinkedBlockingDeque<E>> localDeques = new ConcurrentHashMap<Long, LinkedBlockingDeque<E>>();
	protected AtomicInteger remainingCapacity = null;
	protected int capacity = Integer.MAX_VALUE;
	protected int chunksize = 1;
	
	/**
	 * Create an empty WorkStealingQueue with maximum capacity and chunksize of 1.
	 * @see #WorkStealingQueue(int, int)
	 * @see #WorkStealingQueue(Collection, int)
	 */
	public WorkStealingQueue() {
		remainingCapacity = new AtomicInteger(capacity);
	}
	
	/**
	 * Create an empty WorkStealingQueue with the specified capacity and chunksize. Chunksize refers to the 
	 * number of elements stolen at a time, in the case of a steal.  
	 * @param capacity		The WorkStealingQueue's capacity
	 * @param chunksize		The chunksize in case of steals
	 * 
	 * @see #WorkStealingQueue()
	 * @see #WorkStealingQueue(Collection, int)
	 */
	public WorkStealingQueue(int capacity, int chunksize) {
		if (chunksize < HALF || chunksize == 0)
			throw new IllegalArgumentException("Invalid chunksize: "+chunksize);
		if (capacity <= 0)
			throw new IllegalArgumentException("Invalid capacity: "+capacity);
		this.capacity = capacity;
		this.chunksize = chunksize;
		remainingCapacity = new AtomicInteger(capacity);
	}
	
	/**
	 * Create a WorkStealingQueue that contains the specified collection of elements and specified chunksize.
	 * The capacity defaults to unlimited.
	 * @param c	 The collection of elements to place inside the WorkStealingQueue
	 * @param chunksize		The chunksize in case of steals
	 */
	public WorkStealingQueue(Collection<? extends E> c, int chunksize) {
		if (chunksize < HALF || chunksize == 0)
			throw new IllegalArgumentException("Invalid chunksize: "+chunksize);
		remainingCapacity = new AtomicInteger(capacity);
		this.chunksize = chunksize;
		addAll(c);
	}
	
	@Override
	public boolean add(E e) {
		if (remainingCapacity.get() <= 0)
			throw new IllegalStateException("Cannot add element, collection capacity exceeded.");
		
		remainingCapacity.decrementAndGet();
		
		long id = Thread.currentThread().getId();
		LinkedBlockingDeque<E> local = localDeques.get(id);
		
		if (local == null) {
			// -- first-time thread, needs a local queue
			local = new LinkedBlockingDeque<E>();
			localDeques.put(id, local);
		}
		
		local.addFirst(e);
		return true;
	}
	
	@Override
	public boolean contains(Object o) {
		Iterator<LinkedBlockingDeque<E>> it = localDeques.values().iterator();
		while (it.hasNext()) {
			LinkedBlockingDeque<E> q = it.next();
			if (q.contains(o))
				return true;
		}
		return false;
	}
	
	@Override
	public int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}
	
	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == this)
			throw new IllegalArgumentException("Cannot drain to itself.");

		int amountLeftToDrain = maxElements;
		int amountDrained = 0;
		
		Iterator<LinkedBlockingDeque<E>> it = localDeques.values().iterator();
		while (it.hasNext()) {
			LinkedBlockingDeque<E> q = it.next();
			int d = q.drainTo(c, amountLeftToDrain);
			amountDrained += d;

			if (maxElements != Integer.MAX_VALUE)
				amountLeftToDrain -= d;
		}
		remainingCapacity.getAndAdd(amountDrained);
		return amountDrained;
	}
	
	@Override
	public boolean offer(E e) {
		try {
			return add(e);
		} catch (IllegalStateException exc) {
			return false;
		}
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		if (offer(e))
			return true;

		long startTime = System.currentTimeMillis();
		long timeOutMilli = unit.toMillis(timeout);
		
		while ((System.currentTimeMillis()-startTime) <= timeOutMilli) {
			Thread.sleep(sleep_amount_milli);
			if (offer(e))
				return true;
		}
		return false;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		E e = poll();
		
		if (e != null)
			return e;
		
		long startTime = System.currentTimeMillis();
		long timeOutMilli = unit.toMillis(timeout);
		
		while ((System.currentTimeMillis()-startTime) <= timeOutMilli) {
			Thread.sleep(sleep_amount_milli);
			e = poll();
			if (e != null)
				return e;
		}
		return null;
	}

	@Override
	public void put(E e) throws InterruptedException {
		while (true) {
			try {
				add(e);
			} catch (IllegalStateException exc) {
				Thread.sleep(sleep_amount_milli);
			}
		}
	}

	@Override
	public int remainingCapacity() {
		return remainingCapacity.get();
	}

	@Override
	public boolean remove(Object o) {
		Iterator<LinkedBlockingDeque<E>> it = localDeques.values().iterator();
		while (it.hasNext()) {
			LinkedBlockingDeque<E> q = it.next();
			if (q.remove(o)) {
				remainingCapacity.incrementAndGet();
				return true;
			}
		}
		return false;
	}

	@Override
	public E take() throws InterruptedException {
		while (true) {
			E e = poll();
			if (e != null)
				return e;
			Thread.sleep(sleep_amount_milli);
		}
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
	public E peek() {
		try {
			return element();
		} catch (NoSuchElementException exc) {
			return null;
		}
	}
	
	@Override
	public E poll() {
		
		//-- check own local queue first
		E e = pollLocalQueue();
		if (e != null)
			return e;
		
		//-- try to steal an element
		e = attemptToStealRandom();
		if (e != null) {
			return e;
		}
		
		//-- found nothing
		return null;
	}
	
	protected E pollLocalQueue() {
		long id = Thread.currentThread().getId();
		LinkedBlockingDeque<E> deque = localDeques.get(id);
		
		if (deque != null) {
			//-- try to get an element from the local deque
			E e = deque.pollFirst();
			if (e != null) {
				remainingCapacity.incrementAndGet();
				return e;
			}
		} else {
			//-- a deque doesn't exist for this thread, so create one
			deque = new LinkedBlockingDeque<E>();	
			localDeques.put(id, deque);
		}
		return null;
	}
	
	protected E attemptToStealNonRandom() {
		//-- try to steal from the other threads -- NOT Randomised 
		Iterator<LinkedBlockingDeque<E>> otherDeques = localDeques.values().iterator();
		while (otherDeques.hasNext()) {
			LinkedBlockingDeque<E> q = otherDeques.next();
			E e = q.pollLast();
			if (e != null) {
				remainingCapacity.incrementAndGet();
				return e;
			}
		}
		return null;
	}
	
	protected E attemptToStealRandom() {
		LinkedBlockingDeque<E>[] otherDeques = localDeques.values().toArray(new LinkedBlockingDeque[]{});
		int numThreads = otherDeques.length;
		
		int startVictim = (int) (Math.random()*numThreads);
		
		for (int v = 0; v < numThreads; v++) {
			int nextVictim = (startVictim+v)%numThreads;
			
			LinkedBlockingDeque<E> victimQueue = otherDeques[nextVictim];
			E e = victimQueue.pollLast();
			if (e != null) {
				
				//-- if chunksize is not 1, attempt to steal more elements
				if (chunksize != 1) {
//					int amountStolen = 1;
					
					LinkedBlockingDeque<E> thiefDeque = localDeques.get(Thread.currentThread().getId());
					//-- if managed to steal one element, try to steal chunksize-1 more (in total stealing chunksize elements)
					if (thiefDeque != victimQueue) {
						int maxElements = chunksize-1;
						if (chunksize == HALF) {
							maxElements = victimQueue.size()/2;
						}
						victimQueue.drainTo(thiefDeque, maxElements);
					}
				}
				
				remainingCapacity.incrementAndGet();
				return e;
			}
		}
		return null;
	}

	@Override
	public E remove() {
		E e = poll();
		if (e == null)
			throw new NoSuchElementException("No element found in collection.");
		return e;
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		Iterator<? extends E> it = c.iterator();
		while (it.hasNext())
			add(it.next());
		return true;
	}
	
	@Override
	public void clear() {
		Iterator<LinkedBlockingDeque<E>> it = localDeques.values().iterator();
		while (it.hasNext()) {
			LinkedBlockingDeque<E> q = it.next();
			q.clear();
		}
		remainingCapacity.set(capacity);
	}
	
	@Override
	public boolean containsAll(Collection<?> c) {
		return asList().containsAll(c);
	}
	
	@Override
	public boolean isEmpty() {
		Iterator<LinkedBlockingDeque<E>> it = localDeques.values().iterator();
		while (it.hasNext()) {
			LinkedBlockingDeque<E> q = it.next();
			if (!q.isEmpty())
				return false;
		}
		return true;
	}
	
	@Override
	public Iterator<E> iterator() {
		return asList().iterator();
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		//-- cannot use removeAll() on the underlying queues since we also need to record how many were removed
		
		boolean changed = false;
		Iterator<?> it = c.iterator();
		while (it.hasNext()) {
			Object e = it.next();
			
			// -- remove(Object) only removes one at a time
			while (remove(e))
				changed = true;
		}
		return changed;
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		//-- cannot use retainAll() on the underlying queues since we need to update capacityRemaining (done inside remove(Object))
		
		boolean changed = false;
		Iterator<E> it = asList().iterator();
		while (it.hasNext()) {
			E e = it.next();
			if (!c.contains(e)) {
				if (remove(e))
					changed = true;
			}
		}
		return changed;
	}

	@Override
	public int size() {
		int size = 0;
		
		Iterator<LinkedBlockingDeque<E>> it = localDeques.values().iterator();
		while (it.hasNext()) {
			LinkedBlockingDeque<E> q = it.next();
			size += q.size();
		}
		return size;
	}
	
	@Override
	public Object[] toArray() {
		return asList().toArray();
	}
	
	@Override
	public <T> T[] toArray(T[] a) {
		return asList().toArray(a);
	}
	
	protected ArrayList<E> asList() {
		ArrayList<E> list = new ArrayList<E>();
		Iterator<LinkedBlockingDeque<E>> it = localDeques.values().iterator();
		while (it.hasNext()) {
			LinkedBlockingDeque<E> q = it.next();
			list.addAll(q);
		}
		return list;
	}
}
