package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;

	static Condition2 sleepOahuAdult;
	static Condition2 sleepOahuChild;
	static Condition2 sleepMolokaiChild;
	static Condition2 sleepOnBoat;

	static Lock adultLock;
	static Lock childLock;
	static Lock boatLock;
	static Lock molokaiLock;

	static boolean boatMolokai;
	static boolean adultTurn;

	static int countOahuAdult;
	static int countOahuChild;
	static int countActiveChildren;
	static int countBoat;

	static Semaphore finish;

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
		adultLock = new Lock();
		childLock = new Lock();
		molokaiLock = new Lock();
		boatLock = new Lock();

		sleepOahuAdult = new Condition2(adultLock);
		sleepOahuChild = new Condition2(childLock);
		sleepMolokaiChild = new Condition2(molokaiLock);
		sleepOnBoat = new Condition2(boatLock);

		Semaphore finish = new Semaphore(0);


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
			t.setName("Sample Boat Thread");
			t.fork();
		}

		for(int i = 0; i < children; i++)
		{
			KThread t = new KThread(c);
			t.setName("Sample Boat Thread");
			t.fork();
		}
		finish.P();
    }

    static void AdultItinerary()
    {
		//Adult threads do not need a onMolokai check, because their threads end when they are on Molokai
		//boolean onMolokai = false;

		//only one adult will be awake at a time, this is due to that adults are what wake children
		adultLock.aquire();
		//don't go to Molokai yet if the boat isn't there or if a child isn't on Molokai
		while(boatMolokai || !adultTurn)
		{
			/*If the boat is on Oahu, but they haven't sent two children
			//This is a contingency wake
			if(!boatMolokai && !adultTurn)
			{
				sleepOahuChild.wake();
				sleepOahuChild.wake();
			}*/
			sleepOahuAdult.sleep();
		}
		//Adult rows to Molokai

		//The adults will know that there are no longer any children on Molokai,
		//as the returning boat is the second child
		adultTurn = false;

		countOahuAdult--;
		boatMolokai = true;
		bg.AdultRowToMolokai();
		//wake a child to row the boat back to Oahu
		sleepMolokaiChild.wake();

		adultLock.release();
    }

    static void ChildItinerary()
    {
		//INITIALIZATION
		boolean onMolokai = false;
		boolean finalVoyage = false;
		boolean passenger;

		while(true)
		{
			passenger = false;
			if(!onMolokai)
			{
				//if the boat isn't there or if an adult is going to use the boat next,
				//then the child should sleep. No more than two children should ever be active
				childLock.aquire();
				while(boatMolokai || adultTurn || countActiveChildren >= 2)
				{
					sleepOahuChild.sleep();
				}
				countActiveChildren++;
				childLock.release();

				//If there are only 2 children left on the island, then this is the final voyage
				//This check is done before sailing, to simulate only information known to people on the island
				if(countOahuChild <= 2 && countOahuAdults <= 0)
					finalVoyage = true;

				boatlock.aquire();
				//If there are zero children waiting on the boat...
				if(countBoat == 0)
				{
					//If the not the final child, wait on the boat and sleep
					if(countOahuChild > 1)
					{
						countBoat++;
						sleepOnBoat.sleep();
					}
					//If the final Child on the island, just row and finish the simulation
					else
					{
						bg.ChildRowToMolokai();
						boatLock.release();
						finish.V();
						break;
					}
				}
				//If the second child on the boat, the child is now the passenger
				//Wake the child that was waiting for a passenger
				else
				{
					passenger = true;
					countBoat++;
					sleepOnBoat.wake();
				}
				boatLock.release();

				//If there are still adults on the island, then they will take the boat next
				if(countOahuAdult > 0)
						adultTurn=true;

				//Set counters and send messages that the two boat children are now in Molokai
				countOahuChild--;
				onMolokai = true;
				//Okay to decrement active children without lock, because the
				// while loop is still true with the boat now at Molokai.
				boatMolokai = true;
				countActiveChildren--;
				//state that the child is going to Molokai, whether as a passenger or as a rower
				if(passenger)
					bg.ChildRideToMolokai();
				else
					bg.ChildRowToMolokai();

				//if the child is the last one off the boat, they will instead return back to Oahu
				//otherwise they will sleep
				molokaiLock.aquire();
				countBoat--;
				if(countBoat > 0)
				{
					sleepMolokaiChild.sleep();
				}
				molokaiLock.release();
			}

			//finishes the simulation if no one was on the island after leaving
			//This is done after the lock check to make sure both the row and ride messages were sent
			if(finalVoyage)
			{
				finish.V();
				break;
			}

			if(onMolokai)
			{
				//A child rows back to Oahu by themselves.
				countOahuChild++;
				onMolokai = false;
				boatMolokai = false;

				bg.ChildRowToOahu();

				//If there are adults on  the island and still a child on Molokai, then the adult should go
				//Otherwise wake a child to go to the island again
				if(countOahuAdult > 0 && adultTurn)
				{
					sleepOahuAdult.wake();
					sleepOahuChild.sleep();
				}
				else
					sleepOahuChild.wake();
			}
		}
    }
}
