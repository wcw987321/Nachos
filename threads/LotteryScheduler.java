package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * Project 1 Task 5
 * 2017.3.29
 *
 * If we regard thread A as thread B's father when A holds some resource B needs
 * directly, then they consist of a tree, so we use holdThread to represent the
 * parent, waitThreads to represent the children, waitQueue to represent the
 * ancestors, and holdQueues to represent the descendants. Also, there are only
 * constant possible value of priority, so waitThreads[i] represents the children
 * whose priority is i, and we use TreeSet to store them.
 */

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class LotteryScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public LotteryScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    //public static final int priorityMaximum = 7;
    public static final int priorityMaximum = Integer.MAX_VALUE;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class LotteryQueue extends ThreadQueue {
        LotteryQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
		/*for(int i = priorityMinimum;i <= priorityMaximum;i++)
			waitThreads[i] = new TreeSet<ThreadState>(); */
            waitThreadsSet = new TreeSet<>();
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
            if(transferPriority) //
                holdThread = getThreadState(thread);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            ThreadState temp = pickNextThread(); //
            if(temp == null) //
                return null;
            else
                return temp.thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */

        protected ThreadState NextThread() { //

            int sum = 0;
            for (ThreadState threadState : waitThreadsSet) {
                sum += threadState.getEffectivePriority();
            }

            int tmp = 0;
            int lotteryValue = (new Random()).nextInt(sum) + 1;

            for (ThreadState threadState : waitThreadsSet) {
                tmp += threadState.effectivePriority;
                if (tmp >= lotteryValue) {
                    waitThreadsSet.remove(threadState);
                    return threadState;
                }
            }
            return null;
        }

        protected ThreadState pickNextThread() {
            // implement me
            ThreadState res = NextThread(); //
            if(holdThread!=null) //
            {
                holdThread.holdQueues.remove(this);
                holdThread.getEffectivePriority();
                holdThread=res;
            }
            if(res!=null) //
                res.waitQueue = null;
            return res;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        public void add(ThreadState thread) { //
            //waitThreads[thread.effectivePriority].add(thread);
            waitThreadsSet.add(thread);
        }

        public boolean isEmpty() { //
            return waitThreadsSet.isEmpty();
		/*for(int i = priorityMinimum;i <= priorityMaximum;i++)
			if(!waitThreads[i].isEmpty())
				return false;
		return true;*/
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        protected long cnt = 0; //
        public boolean transferPriority;
        //protected TreeSet<ThreadState> waitThreads[] = new TreeSet[priorityMaximum+1];
        protected TreeSet<ThreadState> waitThreadsSet = new TreeSet<>();
        protected ThreadState holdThread = null; //

    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState /**/implements Comparable<ThreadState>/**/{
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            setPriority(priorityDefault);
            getEffectivePriority(); //
        }

	/*public int compareTo(ThreadState target) { //
		if(time > target.time)
			return 1;
		else if(time == target.time)
			return 0;
		else
			return -1;
	}*/

        public int compareTo(ThreadState target) {
            if (this.effectivePriority > target.effectivePriority)
                return 1;
            else if (this.effectivePriority < target.effectivePriority)
                return -1;
            else {
                if (this.time > target.time)
                    return 1;
                if (this.time < target.time)
                    return -1;
                return 0;
            }
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return	the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return	the effective priority of the associated thread.
         */
        public int getEffectivePriority() { //
            // implement me
            int i,temp = priority;
            if(!holdQueues.isEmpty())
            {
                Iterator<LotteryQueue> iterator=holdQueues.iterator();
                while(iterator.hasNext())
                {
                    LotteryQueue holdQueue = (LotteryQueue)iterator.next();

                    for (ThreadState threadState : holdQueue.waitThreadsSet) {
                        temp += threadState.effectivePriority;
                    }

                    /*int maxPriority = 0;
                    if (holdQueue.waitThreadsSet.isEmpty() == false)
                        maxPriority = holdQueue.waitThreadsSet.last().effectivePriority;
                    if (maxPriority > temp)
                        temp = maxPriority;*/
                }
            }
            if(waitQueue!=null&&temp!=effectivePriority)
            {
                //((PriorityQueue)waitQueue).waitThreads[effectivePriority].remove(this);
                //((PriorityQueue)waitQueue).waitThreads[temp].add(this);
                ((LotteryQueue) waitQueue).waitThreadsSet.remove(this);
                this.effectivePriority = temp;
                ((LotteryQueue) waitQueue).waitThreadsSet.add(this);
            }
            if(holdThread!=null)
                holdThread.getEffectivePriority();
            return (effectivePriority=temp);
            //return priority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param	priority	the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;

            // implement me
            getEffectivePriority(); //
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param	waitQueue	the queue that the associated thread is
         *				now waiting on.
         *
         * @see	nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(LotteryQueue waitQueue) { //
            // implement me
            Lib.assertTrue(Machine.interrupt().disabled());
            time=++waitQueue.cnt;
            this.waitQueue=waitQueue;
            waitQueue.add(this);
            holdThread=waitQueue.holdThread;
            getEffectivePriority();
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see	nachos.threads.ThreadQueue#acquire
         * @see	nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(LotteryQueue waitQueue) { //
            // implement me
            Lib.assertTrue(Machine.interrupt().disabled());
            if(waitQueue.transferPriority)
                holdQueues.add(waitQueue);
            Lib.assertTrue(waitQueue.isEmpty());
        }

        /** The thread with which this object is associated. */
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority/**/, effectivePriority;
        protected long time; //
        protected ThreadQueue waitQueue=null; //
        protected LinkedList<LotteryQueue> holdQueues = new LinkedList<>(); //
        protected ThreadState holdThread=null; //
    }
}

