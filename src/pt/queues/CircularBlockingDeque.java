package pt.queues;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CircularBlockingDeque<E> extends LinkedBlockingDeque<E> {
	static final class Node<E> {
	    E item;
        Node<E> prev;
        Node<E> next;
        Node(E x, Node<E> p, Node<E> n) {
            item = x;
            prev = p;
            next = n;
        }
    }
	
    private transient Node<E> first;
    private transient Node<E> last;
    private transient int count;
    private final int capacity;
    
    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notEmpty = lock.newCondition();

    private final Condition notFull = lock.newCondition();

    public CircularBlockingDeque() {
        this(Integer.MAX_VALUE);
    }
    public CircularBlockingDeque(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
    }
    public CircularBlockingDeque(int capacity, Collection<? extends E> c) {
        this(capacity);
        for (E e : c)
            add(e);
    }

    // Add a new element as the first node. (Bottom most node)
    private boolean linkFirst(E e) {
        if (count >= capacity)
            return false;
        ++count;
        Node<E> f = first;
        Node<E> l = last;
        
        if (f != null && l!= null) {
	        Node<E> x = new Node<E>(e, l, f);
	        first = x;
	        l.next = x;
        } else { // It is the first node inserted
        	Node<E> x = new Node<E>(e, null, null);
        	x.prev = x;
        	x.next = x;
        	first = x;
            last = x;
        }
        
        notEmpty.signal();
        return true;
    }
    
    // Return and remove the first (bottom most) element
    private E unlinkFirst() {
        Node<E> f = first;
        if (f == null)
            return null;
        
    	Node<E> n = f.next;
    	Node<E> l = last;
        
        if (count == 1) {
        	first = null;
        	last = null;
        } else {
        	l.next = n;
        	n.prev = l;
        	first = n;
        }
        
        --count;
        notFull.signal();
    	return f.item;
    }
    
    // Return and remove the last (top most) element
    private E unlinkLast() {
        Node<E> l = last;
        if (l == null)
            return null;
        
        Node<E> p = l.prev;
        Node<E> f = first;
        
        if (count == 1) {
        	first = null;
        	last = null;
        } else {
        	f.prev = p;
        	p.next = f;
        	last = p;
        }
        
        --count;
        notFull.signal();
        return l.item;
    }    
    
    // Deque methods

    @Override
    public boolean offerFirst(E e) {
        if (e == null) throw new NullPointerException();
        lock.lock();
        try {
            return linkFirst(e);
        } finally {
            lock.unlock();
        }
    }
    
    public CircularBlockingDeque<E> grow() {
    	CircularBlockingDeque<E> cbd = new CircularBlockingDeque<E>(this.capacity+1);
    	for (Node<E> p = last; p != first; p = p.prev) 
    		cbd.addFirst(p.item);
    	cbd.addFirst(first.item);
    	return cbd;
    }
    
    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        lock.lock();
        try {
            for (Node<E> p = first; p != last; p = p.next)
                if (o.equals(p.item))
                    return true;
            
            if (o.equals(last.item))
                return true;
            
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E remove() {
        lock.lock();
        try {
            return unlinkFirst();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E peekFirst() {
        lock.lock();
        try {
            return (first == null) ? null : first.item;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E peekLast() {
        lock.lock();
        try {
            return (last == null) ? null : last.item;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E pollFirst() {
        lock.lock();
        try {
            return unlinkFirst();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E pollLast() {
        lock.lock();
        try {
            return unlinkLast();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public boolean isEmpty() {
    	if (first == null) 
    		return true;    	
    	return false;
    }
    

	public Iterator<E> iterator() {
	    return new CircularIterator();
	}
    
    private class CircularIterator implements Iterator<E> {

        private Node<E> n;
        private boolean atStart;

        public CircularIterator() {
            if(!isEmpty()) {
                n = first;
                atStart = true;
            }
        }

        @Override
        public boolean hasNext() { 
            if(isEmpty() || n == first && !atStart) {
                return false;
            }
            return true;
        }

        @Override
        public E next() {
            E item = n.item;
            atStart = false;
            n = n.next;
            return item;
        }
    } 
}
