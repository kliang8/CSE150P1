package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {

        private Lock lock;

        private Condition2 cSpeaker;
        private Condition2 cListener;

        private int speakerCount = 0;
        private int listenerCount = 0;

        private int word = 0;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
           lock = new Lock();
           cSpeaker =  new  Condition2(lock);
           cListener =  new  Condition2(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	// Getting the lock
    	lock.acquire();
    	// Increasing number of Speakers
    	speakerCount++;
    	//Wait for thread to listen through for this communicator
    	if (listenerCount < 1)
    		cSpeaker.sleep();
    	//Transfer word to listener
    	this.word = word;
        cListener.wakeAll()
        speakerCount--;
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
    	// Wait for thread to speak then return word that is passed.
    	int msg;
    	lock.acquire();
    	listenerCount++;
    	while(this.word == 0) {
    		cSpeaker.notify();
    		cListener.sleep();
    	}
    	msg = this.word;
        this.word = 0;
    	listenerCount--;
    	lock.release();
	return msg;
    }
}
