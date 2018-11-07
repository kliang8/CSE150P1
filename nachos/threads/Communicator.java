package nachos.threads;
import java.util.LinkedList;
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
        private Condition2 cReady;

        private int speakerCount = 0;

        private boolean validMessage;
        private int message;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
           lock = new Lock();

           cSpeaker = new Condition2(lock);
           cListener = new Condition2(lock);
           cReady = new Condition2(lock);

           validMessage = false;
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
            try {
                    // Making sure word is not valid or no listeners before going to sleep
                    while (validMessage || listenerCount == 0) {
                            cListener.wake();
                            cSpeaker.sleep();
                    }
                    //Transfer word to listener
                    this.message = word;
                    validMessage = true;
                    cListener.wakeAll();

                    // Since next thread might not be the one we want we wait for our corresponding one
                    while (validMessage) {
                            cReady.sleep();
                    }
            } finally {
                    lock.release();
            }
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
    	int msg;
        // Getting the lock
    	lock.acquire();
        try {
                // Increasing number of Listeners
            	listenerCount++;

                while(!validMessage) {
                        cSpeaker.wakeAll();
            		cListener.sleep();
            	}
                // Save word and Reset the word
            	msg = this.message;
                validMessage = false;
            	listenerCount--;

                cReady.wakeAll();
        } finally {
                lock.release();
        }

        // Return the word
	return msg;
    }
}
