package nachos.threads;

import nachos.machine.*;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });                                                                                                                                                                
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {

	Union union;

	if ((union = priorityQueue.poll()) != null){
	    union.getThread().ready();
	}

	KThread.currentThread().yield();
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

    /** add a priority queue to deal with the sorting */

	class Union{
	    KThread thread;
	    long wakeTime;
	    public Union(KThread thread, long wakeTime){
		this.thread = thread;
		this.wakeTime = wakeTime;
	    }
	    public long getWakeTime(){
		return this.wakeTime;
	    }
	    public KThread getThread(){
		return this.thread;
	    }
	}

	Comparator<Union> OrderIsdn =  new Comparator<Union>(){
	    public int compare(Union u1, Union u2){
		long wakeTime1 = u1.getWakeTime();
		long wakeTime2 = u2.getWakeTime();
		if (wakeTime1 > wakeTime2) {return 1;}
		else if (wakeTime1 < wakeTime2) {return -1;}
		else {return 0;}
	    }
	};

	Queue<Union> priorityQueue = new PriorityQueue<Union>(11, OrderIsdn);

	

    public void waitUntil(long x) {

	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;
	//while (wakeTime > Machine.timer().getTime())
	//    KThread.yield();

	boolean intStatus = Machine.interrupt().disable();

	KThread thread = KThread.currentThread();

	Union union = new Union(thread, wakeTime);

	priorityQueue.add(union);

	KThread.sleep();

	Machine.interrupt().restore(intStatus);
	
    }
}
