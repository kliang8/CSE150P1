package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	
	/**
	* Creates a priority queue
	*/
	private PriorityQueue<SleepingThread> sThreadQueue;
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() 
	{
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() { timerInterrupt(); }
			});
		
		sThreadQueue = new PriorityQueue<SleepingThread>();
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() 
	{
		boolean intStatus = Machine.interrupt().enabled();
		
		//loops until Once the queue is empty or a thread isn't ready to activate
		//because the queue is ordered, can stop checking once one thread isn't ready
		while(!sThreadQueue.isEmpty() && 
		(sThreadQueue.peek().getWakeTime() <=Machine.timer().getTime() ) )
		{
			if(Machine.interrupt().enabled())
				intStatus = Machine.interrupt().disable();
			
			//re-enables the thread to awaken once ready
			(sThreadQueue.remove()).getsThread().ready();
		}
		
		//re-enables the the interrupt, but will not do anything if no interrupt
		Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) 
	{
		long wakeTime = Machine.timer().getTime() + x;
		//starts the interrupt
		boolean intStatus = Machine.interrupt().disable();
		
		//adds to a sorted list of sleeping threads, based on remaining wait time
		SleepingThread thread = new SleepingThread(KThread.currentThread(), wakeTime);
		sThreadQueue.add(thread);
		
		//disables the current thread and moves to the next thread, and starts again
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
    }
	
	public class SleepingThread implements Comparable <SleepingThread>
	{
		private KThread sThread;
		private long wakeTime;
		
		public SleepingThread(KThread k, long wT)
		{
			sThread = k;
			wakeTime = wT;
		}
		
		public long getWakeTime(){return wakeTime;}
		
		public KThread getsThread(){return sThread;}
		
		public int compareTo(SleepingThread other)
		{
			return Long.compare(wakeTime, other.getWakeTime());
		}
	}
	
}
