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
    private boolean messageWritten;
    private Lock conditionLock;
    private Condition speakerCondition, listenerCondition, speakerReturnCondition;
    public Communicator() {
    	this.message = 0;
    	this.numOfSpeaker = 0;
    	this.numOfListener = 0;
	this.messageWritten = false;
    	this.conditionLock = new Lock();
    	this.speakerCondition = new Condition(this.conditionLock);
    	this.listenerCondition = new Condition(this.conditionLock);
	this.speakerReturnCondition = new Condition(this.conditionLock);
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
	//System.out.println("conditionLock acquired by speaker");
	while ((numOfListener == 0) || (messageWritten)){
	    numOfSpeaker += 1;
	    //System.out.println("numOfListener: "+numOfListener);
	    //System.out.println("numOfSpeaker: "+numOfSpeaker);
	    //System.out.println("speaker go to sleep");
	    speakerCondition.sleep();
	    //System.out.println("speaker wake up");
	}
	//System.out.println(numOfListener);
	message = word;
	//System.out.println("written " + word);
	messageWritten = true;
	numOfListener = 0;
	//System.out.println("numOfListener become 0");
	listenerCondition.wakeAll();
	//System.out.println("speaker go to sleep");
	speakerReturnCondition.sleep();
	//System.out.println("speaker wake up");
	//System.out.println("speaker " + word + " returned.");
	//System.out.println("conditionLock released by speaker");
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
	//System.out.println("conditionLock acquired by listener");
	while (messageWritten == false){
	    numOfListener += 1;
	    //System.out.println("numOfListener: " + numOfListener);
	    //System.out.println("listener go to sleep");
	    speakerCondition.wakeAll();
	    listenerCondition.sleep();
	    //System.out.println("listener wake up");
	}
	int word = message;
	//System.out.println("read " + word);
	messageWritten = false;
	numOfSpeaker = 0;
	speakerCondition.wakeAll();
	speakerReturnCondition.wake();
	//System.out.println("listener " + word + " returned.");
	//System.out.println("conditionLock released by listener");
	conditionLock.release();
	return word;
    }
}
