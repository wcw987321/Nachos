package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    static final int Oahu = 0;
    static final int Molokai = 1;
    static int boatPosition = 0;
    static int numOfChildrenOnMolokai = 0;
    static int numOfAdultsOnOahu = 0;
    static int numOfChildrenOnOahu = 0;
    static int peopleOnBoat = 0;
    static int m = 0;
    static Lock conditionLock = new Lock();
    static Condition OahuChildCondition = new Condition(conditionLock);
    static Condition OahuAdultCondition = new Condition(conditionLock);
    static Condition MolokaiChildCondition = new Condition(conditionLock);
    static Condition boatCondition = new Condition(conditionLock);
    static Alarm alarm = new Alarm();
    static Semaphore s1 = new Semaphore(0);
    static Semaphore s2 = new Semaphore(0);
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	//System.out.println("\n ***Testing Boats with only 2 children***");
	//begin(16, 16, b);

	//alarm.waitUntil(10000);

	//begin(0, 16, b);

	for (int i = 0; i < 7; i++) for (int j = 2; j < 7; j++) {System.out.println(i+" " + j); begin(i, j, b);}

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/

    boatPosition = 0;
    numOfChildrenOnMolokai = 0;
    numOfAdultsOnOahu = 0;
    numOfChildrenOnOahu = 0;
    peopleOnBoat = 0;
    m = 0;
	
	Communicator com = new Communicator();
	class Adult implements Runnable{
	    private Communicator com;
	    public Adult(Communicator com){
		this.com = com;
	    }
	    public void run(){
		AdultItinerary(com);
	    }
	}

	class Child implements Runnable{
	    private Communicator com;
	    public Child(Communicator com){
		this.com = com;
	    }
	    public void run(){
		ChildItinerary(com);
	    }
	}

	for (int i = 0; i < adults; i++){
	    new KThread(new Adult(com)).fork();
	}

	for (int i = 0; i < children; i++){
	    new KThread(new Child(com)).fork();
	}

	int sum = adults + children;

	/*while(m < sum){
	    //System.out.println("begin to listen");
	    //s1.V();
	    //System.out.println("release s1");
	    //s2.P();
	    //System.out.println("get s2");
	    //m += com.listen();
	    //alarm.waitUntil(1000);
	    //System.out.println("m: " + m);
	}*/

	s1.P();
	//System.out.println("main function finished!");
    }

    static void AdultItinerary(Communicator com)
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE.

	conditionLock.acquire();
	numOfAdultsOnOahu += 1;
	OahuAdultCondition.sleep();
	//System.out.println("Adult wake up.");
	while ((boatPosition != Oahu) || (peopleOnBoat > 0)) OahuAdultCondition.sleep();
	bg.AdultRowToMolokai();
	numOfAdultsOnOahu -= 1;
	boatPosition = Molokai;
	MolokaiChildCondition.wake();
	//com.speak(1);
	//s1.P();
	//System.out.println("get s1");
	//m += 1;
	//s2.V();
	//System.out.println("release s2");
	conditionLock.release();

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    }

    static void ChildItinerary(Communicator com)
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	int cache = 0;
	int position = Oahu;
	numOfChildrenOnOahu += 1;
	while (true){
	    conditionLock.acquire();
	    if (position == Oahu){
		while ((boatPosition != Oahu) || (peopleOnBoat > 1)) OahuChildCondition.sleep();
		if (peopleOnBoat == 0){
		    //System.out.println("first child on boat");
		    peopleOnBoat += 1;
		    numOfChildrenOnOahu -= 1;
		    OahuChildCondition.wakeAll();
		    //System.out.println("sleep on boat");
		    boatCondition.sleep();
		    position = Molokai;
		    numOfChildrenOnMolokai += 1;
		    MolokaiChildCondition.sleep();
		}
		else{
		    boatCondition.wake();
		    numOfChildrenOnOahu -= 1;
		    //System.out.println("speak 0 to main");
		    //com.speak(0);
		    alarm.waitUntil(1000);
		    cache = numOfChildrenOnOahu + numOfAdultsOnOahu;
		    //KThread.currentThread().yield();
		    //s1.P();
		    //System.out.println("get s1");
		    //m += 0;
		    //s2.V();
		    //System.out.println("release s2");
		    //System.out.println("speak 0 to main returned");
		    bg.ChildRowToMolokai();
		    //System.out.println("speak 1 to main");
		    //com.speak(1);
		    //s1.P();
		    //System.out.println("get s1");
		    //m += 1;
		    //s2.V();
		    //System.out.println("release s2");
		    //System.out.println("speak 1 to main returned");
		    bg.ChildRideToMolokai();
		    OahuChildCondition.wakeAll();
		    boatPosition = Molokai;
		    peopleOnBoat = 0;
		    //System.out.println("speak 1 to main");
		    //com.speak(1);
		    //s1.P();
		    //System.out.println("get s1");
		    //m += 1;
		    //s2.V();
		    //System.out.println("release s2");
		    //System.out.println("speak 1 to main returned");
		    position = Molokai;
		    //cache = numOfChildrenOnOahu + numOfAdultsOnOahu;
		    //System.out.println("cache: " + cache);
		    if (cache > 0){
			bg.ChildRowToOahu();
			position = Oahu;
			boatPosition = Oahu;
			numOfChildrenOnOahu += 1;
			OahuAdultCondition.wake();
			//com.speak(0);
			//s1.V();
			//s1.P();
			//m += -1;
			//s2.V();
			OahuChildCondition.sleep();
		    }
		    else{
			s1.V();
			MolokaiChildCondition.sleep();
		    }
		}
	    }
	    else{
		numOfChildrenOnOahu += 1;
		numOfChildrenOnMolokai -= 1;
		position = Oahu;
		bg.ChildRowToOahu();
		boatPosition = Oahu;
		//System.out.println("gg");
		//System.out.println("begin to speak to main");
		//com.speak(-1);
		//System.out.println("gg");
		//s1.P();
		//m += -1;
		//s2.V();
		OahuChildCondition.wakeAll();
	    }
	    conditionLock.release();
	}
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
