package nachos.threads;

import nachos.machine.*;

import java.util.*;

//Need to overwrite getEffectivePriority() and nextThread() methods.
/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {

    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        // implement me
        return new LotteryQueue(transferPriority);
    }

    protected LotteryThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new LotteryThreadState(thread);

        return (LotteryThreadState)thread.schedulingState;
    }


    protected class LotteryQueue extends PriorityQueue {
        LotteryQueue(boolean transferPriority) {
            super(transferPriority);
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

        protected LotteryThreadState NextThread() {
            int sum = 0;
           for (ThreadState threadState : waitThreadsSet) {
               sum += ((LotteryThreadState)threadState).getEffectivePriority();
           }

            Random rand = new Random();
            int lotteryValue = rand.nextInt(sum) + 1;

            int tmp = 0;

            for (ThreadState threadState : waitThreadsSet) {
                tmp += threadState.effectivePriority;
                if (tmp >= lotteryValue) {
                    return (LotteryThreadState)threadState;
                }
            }

            return null;
        }

        protected LotteryThreadState pickNextThread() {
            // implement me
            LotteryThreadState res = NextThread(); //
            if(holdThread!=null) //
            {
                holdThread.holdQueues.remove(this);
                ((LotteryThreadState)holdThread).getEffectivePriority();
                holdThread=res;
            }
            if(res!=null) //
                res.waitQueue = null;
            return res;
        }
    }

    protected class LotteryThreadState extends PriorityScheduler.ThreadState {

        LotteryThreadState(KThread thread) {
            super(thread);
        }

        public int getEffectivePriority() { //
            // implement me
            int i,temp = priority;
            if(!holdQueues.isEmpty())
            {
                Iterator<PriorityQueue> iterator=holdQueues.iterator();
                while(iterator.hasNext())
                {
                    PriorityQueue holdQueue = (PriorityQueue)iterator.next();
                    /*for(i = priorityMaximum;i > temp;i--) {
                        if (!holdQueue.waitThreads[i].isEmpty()) {
                            temp = i;
                            break;
                        }
                    }*/
                    /*int maxPriority = 0;
                    if (holdQueue.waitThreadsSet.isEmpty() == false)
                        maxPriority = holdQueue.waitThreadsSet.last().effectivePriority;
                    if (maxPriority > temp)
                        temp = maxPriority;*/
                    for (PriorityScheduler.ThreadState prioritySchedulerThreadState : holdQueue.waitThreadsSet) {
                        LotteryThreadState threadState = (LotteryThreadState)prioritySchedulerThreadState;
                        temp += threadState.effectivePriority;
                    }
                }
            }
            if(waitQueue!=null&&temp!=effectivePriority)
            {
                //((PriorityQueue)waitQueue).waitThreads[effectivePriority].remove(this);
                //((PriorityQueue)waitQueue).waitThreads[temp].add(this);
                ((PriorityQueue) waitQueue).waitThreadsSet.remove(this);
                this.effectivePriority = temp;
                ((PriorityQueue) waitQueue).waitThreadsSet.add(this);
            }
            if(holdThread!=null)
                ((LotteryThreadState)holdThread).getEffectivePriority();
            return (effectivePriority=temp);
            //return priority;
        }
    }
}
