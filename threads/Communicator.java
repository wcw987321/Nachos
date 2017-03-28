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
    /**
     * Allocate a new communicator.
     */
    private int message;
    private int numOfSpeaker, numOfListener;
    private Lock conditionLock;
    private Condition speakerCondition, listenerCondition;
    public Communicator() {
    	this.message = 0;
    	this.numOfSpeaker = 0;
    	this.numOfListener = 0;
    	this.conditionLock = new Lock();
    	this.speakerCondition = new Condition(this.conditionLock);
    	this.listenerCondition = new Condition(this.conditionLock);
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
	conditionLock.acquire();
	while (numOfListener == 0){
	    numOfSpeaker += 1;
	    speakerCondition.sleep();
	}
	message = word;
	numOfListener = 0;
	listenerCondition.wakeAll();
	conditionLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
	conditionLock.acquire();
	while (numOfSpeaker == 0){
	    numOfListener += 1;
	    listenerCondition.sleep();
	}
	int word = message;
	numOfSpeaker = 0;
	speakerCondition.wakeAll();
	conditionLock.release();
	return word;
    }
}
