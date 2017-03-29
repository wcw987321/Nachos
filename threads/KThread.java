package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {
	Lib.assertTrue(currentThread != null);
	return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
	if (currentThread != null) {
	    tcb = new TCB();
	}	    
	else {
	    readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	    readyQueue.acquire(this);	    

	    currentThread = this;
	    tcb = TCB.currentTCB();
	    name = "main";
	    restoreState();

	    createIdleThread();
	}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
	this();
	this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
	Lib.assertTrue(status == statusNew);
	
	this.target = target;
	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
	    return -1;
	else if (id > thread.id)
	    return 1;
	else
	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
	Lib.assertTrue(status == statusNew);
	Lib.assertTrue(target != null);
	
	Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);

	boolean intStatus = Machine.interrupt().disable();

	tcb.start(new Runnable() {
		public void run() {
		    runThread();
		}
	    });

	ready();
	
	Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
	begin();
	target.run();
	finish();
    }

    private void begin() {
	Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	Lib.assertTrue(this == currentThread);

	restoreState();

	Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
	
	Machine.interrupt().disable();

	Machine.autoGrader().finishingCurrentThread();

	Lib.assertTrue(toBeDestroyed == null);
	toBeDestroyed = currentThread;


	currentThread.status = statusFinished;

	currentThread.joinSem.V(); /** tell others I have finished */

	sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	
	Lib.assertTrue(currentThread.status == statusRunning);
	
	boolean intStatus = Machine.interrupt().disable();

	currentThread.ready();

	runNextThread();
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());

	if (currentThread.status != statusFinished)
	    currentThread.status = statusBlocked;

	runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
	Lib.debug(dbgThread, "Ready thread: " + toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(status != statusReady);
	
	status = statusReady;
	if (this != idleThread)
	    readyQueue.waitForAccess(this);
	
	Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
	Lib.debug(dbgThread, "Joining to thread: " + toString());

	Lib.assertTrue(this != currentThread);

	this.joinSem.P(); /** waiting for the thread's finish without busy waiting */

	return;

    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
	Lib.assertTrue(idleThread == null);
	
	idleThread = new KThread(new Runnable() {
	    public void run() { while (true) yield(); }
	});
	idleThread.setName("idle");

	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
	KThread nextThread = readyQueue.nextThread();
	if (nextThread == null)
	    nextThread = idleThread;

	nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
	Lib.assertTrue(Machine.interrupt().disabled());

	Machine.yield();

	currentThread.saveState();

	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());

	currentThread = this;

	tcb.contextSwitch();

	currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
	Lib.assertTrue(tcb == TCB.currentTCB());

	Machine.autoGrader().runningThread(this);
	
	status = statusRunning;

	if (toBeDestroyed != null) {
	    toBeDestroyed.tcb.destroy();
	    toBeDestroyed.tcb = null;
	    toBeDestroyed = null;
	}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
	    for (int i=0; i<5; i++) {
		System.out.println("*** thread " + which + " looped "
				   + i + " times");
		currentThread.yield();
	    }
	}

	private int which;
    }
 
    private static class PingTest2 implements Runnable  
    {  
        Lock a=null,b=null;  
        int name;  
        PingTest2(Lock A,Lock B,int x)  
        {  
            a=A;b=B;name=x;  
        }  
        public void run() {  
            System.out.println("Thread "+name+" starts.");  
            if(b!=null)  
            {  
                System.out.println("Thread "+name+" waits for Lock b.");  
                b.acquire();  
                System.out.println("Thread "+name+" gets Lock b.");  
            }  
            if(a!=null)  
            {  
                System.out.println("Thread "+name+" waits for Lock a.");  
                a.acquire();  
                System.out.println("Thread "+name+" gets Lock a.");  
            }  
            KThread.yield();  
            boolean intStatus = Machine.interrupt().disable();  
            System.out.println("Thread "+name+" has priority "+ThreadedKernel.scheduler.getEffectivePriority()+".");  
            Machine.interrupt().restore(intStatus);  
            KThread.yield();  
            if(b!=null) b.release();  
            if(a!=null) a.release();  
            System.out.println("Thread "+name+" finishs.");  
              
        }  
    }  


    private static class CommunicateTest implements Runnable {
	CommunicateTest(boolean flag, Communicator com, int word) {
	    this.flag = flag;
	    this.com = com;
	    this.word = word;
	}

	public void run() {
	    if (flag) {for (int i = 0; i < 10; i++) com.speak(word);}
	    else {for (int i = 0; i < 10; i++) System.out.println(com.listen());}
	}

	private boolean flag;
	private Communicator com;
	private int word;
    }

    private static class AlarmTest implements Runnable {
	AlarmTest(Alarm alarm, long time, int num) {
	    this.alarm = alarm;
	    this.time = time;
	    this.num = num;
	}

	public void run()
	{
    		System.out.println(num + " previous time: " + Machine.timer().getTime());
  		//ThreadedKernel.alarm.waitUntil(1000);
		alarm.waitUntil(time);
    		System.out.println(num + " posterior time: " + Machine.timer().getTime());
    	}

	private Alarm alarm;
	private long time;
	private int num;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {

	/** original test */

	Lib.debug(dbgThread, "Enter KThread.selfTest");
	
	new KThread(new PingTest(1)).setName("forked thread").fork();
	new PingTest(0).run();

	/** test of task I */
	PingTest testThread = new PingTest(2);
	KThread testKThread = new KThread(testThread);
	testKThread.fork();
	testKThread.join();

	/**test of task II*/

	Lock lock = new Lock();
    	Condition2 condition2 = new Condition2(lock);
    	Runnable runnableA = new Runnable()
    	{
    		public void run()
    		{
    			lock.acquire();
    			//KThread.currentThread().yield();
    			condition2.wake();
    			System.out.println("thread A begin");
			System.out.println("A sleep");
    			condition2.sleep();
			System.out.println("A wake");
    			lock.release();
    			System.out.println("thread A end");
    		}
    	}, runnableB = new Runnable()
    	{
    		public void run()
    		{
    			lock.acquire();
    			//KThread.currentThread().yield();
    			condition2.wake();
    			System.out.println("thread B begin");
			System.out.println("B sleep");
    			condition2.sleep();
			System.out.println("B wake");
    			lock.release();
    			System.out.println("thread B end");
    		}
    	};
    	new KThread(runnableA).fork();
    	new KThread(runnableB).fork();

	/** test of task III */

	Alarm alarm = new Alarm();
	/*Runnable alarmTest = new Runnable()
    	{
    		public void run()
    		{
    			System.out.println("previous time: " + Machine.timer().getTime());
    			//ThreadedKernel.alarm.waitUntil(1000);
			alarm.waitUntil(1000);
    			System.out.println("posterior time: " + Machine.timer().getTime());
    		}
    	};*/
    	KThread k1 = new KThread(new AlarmTest(alarm, 5000, 1));
    	KThread k2 = new KThread(new AlarmTest(alarm, 4000, 2));
    	KThread k3 = new KThread(new AlarmTest(alarm, 3000, 3));
    	KThread k4 = new KThread(new AlarmTest(alarm, 2000, 4));
    	KThread k5 = new KThread(new AlarmTest(alarm, 1000, 5));
	k1.fork();
	k2.fork();
	k3.fork();
	k4.fork();
	k5.fork();
	k1.join();
	k2.join();
	k3.join();
	k4.join();
	k5.join();

	/** test of task IV */
	Communicator com = new Communicator();
	new KThread(new CommunicateTest(false, com, 1)).fork();
	new KThread(new CommunicateTest(true, com, 2)).fork();
	new KThread(new CommunicateTest(true, com, 3)).fork();
	new KThread(new CommunicateTest(false, com, 4)).fork();
	new KThread(new CommunicateTest(true, com, 5)).fork();
	new KThread(new CommunicateTest(false, com, 6)).fork();
	new KThread(new CommunicateTest(false, com, 1)).fork();
	new KThread(new CommunicateTest(true, com, 2)).fork();
	new KThread(new CommunicateTest(true, com, 3)).fork();
	new KThread(new CommunicateTest(false, com, 4)).fork();
	new KThread(new CommunicateTest(true, com, 5)).fork();
	new KThread(new CommunicateTest(false, com, 6)).fork();
	new KThread(new CommunicateTest(true, com, 7)).fork();
	new CommunicateTest(false, com, 8).run();

	/** test of task V */

	alarm.waitUntil(10000);
	Lock a=new Lock();  
        Lock b=new Lock();  
          
        Queue<KThread> qq=new LinkedList<KThread>();  
        for(int i=1;i<=5;i++)  
        {  
            KThread kk=new KThread(new PingTest2(null,null,i));  
            qq.add(kk);  
            kk.setName("Thread-"+i).fork();  
        }  
        for(int i=6;i<=10;i++)  
        {  
            KThread kk=new KThread(new PingTest2(a,null,i));  
            qq.add(kk);  
            kk.setName("Thread-"+i).fork();  
        }  
        for(int i=11;i<=15;i++)  
        {  
            KThread kk=new KThread(new PingTest2(a,b,i));  
            qq.add(kk);  
            kk.setName("Thread-"+i).fork();  
        }  
        KThread.yield();  
        Iterator it=qq.iterator();  
        int pp=0;  
        while(it.hasNext())  
        {  
            boolean intStatus = Machine.interrupt().disable();  
            ThreadedKernel.scheduler.setPriority((KThread)it.next(),pp+1);  
            Machine.interrupt().restore(intStatus);  
            pp++;
            if(pp>6)pp-=6;
        }  

	/** test of task VI */
	alarm.waitUntil(10000);
	Boat boat = new Boat();
	boat.selfTest();
    }

    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;
    private Semaphore joinSem= new Semaphore(0); /** A semaphore for join and its initialization*/

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;

    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
}
