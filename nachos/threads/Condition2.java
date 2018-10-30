package nachos.threads;
 
import nachos.machine.*;
 
/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2 {
    /**
 	* Allocate a new condition variable.
 	*
 	* @param  conditionLock  the lock associated with this condition
 	*         	variable. The current thread must hold this
 	*         	lock whenever it uses <tt>sleep()</tt>,
 	*         	<tt>wake()</tt>, or <tt>wakeAll()</tt>.
 	*/
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    }
 
    /**
 	* Atomically release the associated lock and go to sleep on this condition
 	* variable until another thread wakes it using <tt>wake()</tt>. The
 	* current thread must hold the associated lock. The thread will
 	* automatically reacquire the lock before <tt>sleep()</tt> returns.
 	*/
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	boolean x = Machine.interrupt().disable(); // Disable interrupts
	KThread currentThread = KThread.currentThread(); // Get the current thread
	waitQueue.waitForAccess(currentThread); // Wait for the currentThread to become accessible
	conditionLock.release(); // conditionLock is released 
	KThread.sleep(); // Thread is put to sleep
	conditionLock.acquire(); // conditionLock is acquired
	Machine.interrupt().restore(x);   // Restore interrupts
  
    }
 
    /**
 	* Wake up at most one thread sleeping on this condition variable. The
 	* current thread must hold the associated lock.
 	*/
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    }
   
	boolean y = Machine.interrupt().disable(); // Disable interrupts
	KThread tmpThread = waitQueue.nextThread(); // Get thread from waitQueue
	if (tmpThread != null){ // If there is thread in queue
   	tmpThread.ready(); // Ready Thread
	}
	Machine.interrupt().restore(y); // Restore interrupts
}
 
    /**
 	* Wake up all threads sleeping on this condition variable. The current
 	* thread must hold the associated lock.
 	*/
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    }
 
	boolean z = Machine.interrupt().disable(); // Disable interrupts
	KThread tmpThread = waitQueue.nextThread(); // Get thread from waitQueue
	tmpThread.ready(); // Ready Thread
   	tmpThread = waitQueue.nextThread(); 
   	
	}
	Machine.interrupt().restore(z); //restore interrupts
 
    }
   
    private Lock conditionLock;
    private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
}
 


