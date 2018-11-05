package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
	
	static Condition2 sleepOahuAdult;
	static Condition2 sleepOahuChild;
	static Condition2 sleepMolokaiChild;
	static Condition2 sleepOnBoat;
	
	static Lock oahuLock;
	static Lock boatLock;
	static Lock molokaiLock;
	
	static boolean boatMolokai;
	static boolean adultTurn;
	
	static int countOahuAdult;
	static int countOahuChild;
	static int countActiveChildren;
	static int countBoat;
	
	static Semaphore finishTest;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

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
		
		//Initialization of values
		boatMolokai = false;
		adultTurn = false;
		countOahuAdult = adults;
		countOahuChild = children;
		countActiveChildren = 0;
		countBoat = 0;
		
		//Initialization of locks and conditions
		oahuLock = new Lock();
		molokaiLock = new Lock();
		
		sleepOahuAdult = new Condition2(oahuLock);
		sleepOahuChild = new Condition2(oahuLock);
		sleepMolokaiChild = new Condition2(molokaiLock);
		sleepOnBoat = new Condition2(oahuLock);
		
		finishTest = new Semaphore(0);
		
		
		Runnable a = new Runnable() {
			public void run() {
				adultItinerary();
			}
		};
			
		Runnable c = new Runnable() {
			public void run() {
				childItinerary();
			}
		};
		for(int i = 0; i < adults; i++)
		{
			KThread t = new KThread(a);
			t.setName("Adult Thread" + i);
			t.fork();
		}
		
		for(int i = 0; i < children; i++)
		{
			KThread t = new KThread(c);
			t.setName("Child Thread" + i);
			t.fork();
		}
		finishTest.P();
    }

    static void adultItinerary()
    {
		//Adult threads do not need a onMolokai check, because their threads end when they are on Molokai
		//boolean onMolokai = false;
		
		//only one adult will be awake at a time, this is due to that adults are what wake children
		oahuLock.acquire();
		//don't go to Molokai yet if the boat isn't there or if a child isn't on Molokai
		while(boatMolokai || !adultTurn)
		{
			/*If the boat is on Oahu, but they haven't sent two children
			//This is a contingency wake
			if(!boatMolokai && !adultTurn)
			{
				sleepOahuChild.wake();
			}*/
			sleepOahuAdult.sleep();
		}
		//Adult rows to Molokai

		//The adults will know that there are no longer any children on Molokai, 
		//as the returning boat is the second child
		adultTurn = false;
		boatMolokai = true;
		countOahuAdult--;
		bg.AdultRowToMolokai();
		oahuLock.release();
		
		//wake a child to row the boat back to Oahu
		molokaiLock.acquire();
		sleepMolokaiChild.wake();
		molokaiLock.release();
    }

    static void childItinerary()
    {
		//INITIALIZATION
		boolean finalVoyage = false;
		boolean adultsFinished = false;
		
		while(true)
		{
			/****************** Oahu Code********************************/
			//if the boat isn't there or if an adult is going to use the boat next,
			//then the child should sleep. No more than two children should ever be active
			oahuLock.acquire();
			while(boatMolokai || adultTurn || countActiveChildren >= 2)
			{
				sleepOahuChild.sleep();
			}
			
			//If there are only 2 children left on the island, then this is the final voyage
			//This check is done before sailing, to simulate only information known to people on the island
			if(countOahuChild <= 2 && countOahuAdult <= 0)
				finalVoyage = true;
			//Also check for if any adults left
			if(countOahuAdult <= 0)
				adultsFinished = true;
			
			//If there are zero children waiting on the boat, wait on the boat and sleep
			if(countActiveChildren == 0)
			{
				//Always will wait for a passenger, because there will always be 2 or more children
				countActiveChildren++;
				sleepOahuChild.wake();
				sleepOnBoat.sleep();
				bg.ChildRowToMolokai();
			}
			//If the second child on the boat, the child is now the passenger
			//Wake the child that was waiting for a passenger
			else
			{
				countActiveChildren++;
				countBoat++;
				sleepOnBoat.wake();
				bg.ChildRideToMolokai();
			}
			
			//Set counters and send messages that the two boat children are now in Molokai
			countOahuChild--;
			countActiveChildren--;
			boatMolokai = true;
			
			//If there are still adults on the island, then they will take the boat next
			if(!adultsFinished)
				adultTurn=true;
			oahuLock.release();
			
			/***********************   Molokai code                     ***********************/
			//if the child is the last one off the boat, they will instead return back to Oahu
			//otherwise they will sleep
			molokaiLock.acquire();
			
			if(countBoat == 0)	
			{
				countBoat++;
				if(adultsFinished)
				{
					molokaiLock.release();
					break;
				}
				sleepMolokaiChild.sleep();
			}
			
			countBoat = 0;

			//finishes the simulation if no one was on the island after leaving
			//This is done after the lock check to make sure both the row and ride messages were sent
			if(finalVoyage)	
			{
				finishTest.V();
				break;
			}
			
			//A child rows back to Oahu by themselves.
			boatMolokai = false;
			bg.ChildRowToOahu();
			molokaiLock.release();
			
			oahuLock.acquire();
			countOahuChild++;
			
			//If there are adults on  the island and still a child on Molokai, then the adult should go
			//Otherwise wake a child to go to the island again
			if(countOahuAdult > 0 && adultTurn)
			{
				sleepOahuAdult.wake();
			}
			oahuLock.release();
		}
    }
}
