/**
 * 
 */
package pt.runtime;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Kingsley
 * @since 31/05/2013
 * 
 * @since 01/10/2013
 * Remove all the unnecessary code before merging.
 *
 */
public class LottoBox {
	/**
	 * <code>lottoNum</code> stands for the number of <b>poisoned</b> threads that a 
	 * specific <code>ThreadPool</code> (especially the <code>OneOff-task</code> thread pool)
	 * knows about. A poisoned thread is a thread that is requested to <i>cancel</i> <b>but is not </b>
	 * <i>cancelled</i> yet! A thread is normally poisoned as a result of changes in the size of 
	 * <code>ThreadPool</code>!
	 * 
	 * @author Mostafa Mehrabi
	 * @since  16/9/2014
	 * */
	protected static AtomicInteger lottoNum = new AtomicInteger(0);
	
	private final static ReentrantLock reentrantLock = new ReentrantLock();
	
	/**
	 * This method allows a poisoned thread (i.e. a thread that is requested to cancel but not cancelled yet)
	 * to try its last chance for letting the thread pool know that it is poisoned. The thread pool will then 
	 * remove this thread from the list of threads.
	 * 
	 * @author Mostafa Mehrabi
	 * @since  15/9/2014
	 * */
	protected static void tryLuck(){
		while (true) {
			if (reentrantLock.tryLock()) {
				WorkerThread workerThread = (WorkerThread) Thread.currentThread();
				if (lottoNum.get() > 0) {
					//Inform Thread Pool
					ThreadPool.lastWords(workerThread.isMultiTaskWorker(), workerThread.getThreadID());
					
					//Count down the latch
					lottoNum.decrementAndGet();
				
					//Set it as a cancelled thread
					workerThread.setCancelled(true);
				}else {
					//No chance to die
					workerThread.requireCancel(false);
				}
				reentrantLock.unlock();
				break;
			}else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * This method is normally called by a <code>Thread Pool</code> (especially <code>OneOff-task</code> thread pools), as
	 * a result of the size of that <code>Thread Pool</code> shrinking, which will consequently cause some of the redundant
	 * threads to be <u>poisoned</u>. This method adds the number of redundant threads to <code>lottoNum</code>
	 * 
	 * @see #lottoNum
	 * @author Mostafa Mehrabi
	 * @since  16/9/2014
	 * */
	protected static void setLotto(int delta){
		lottoNum.addAndGet(delta);
	}
}