package MultiThreadedRestaurantSim;

/**
 * A Machine is used to make a particular Food.  Each Machine makes
 * just one kind of Food.  Each machine has a capacity: it can make
 * that many food items in parallel; if the machine is asked to
 * produce a food item beyond its capacity, the requester blocks.
 * Each food item takes at least item.cookTimeS seconds to produce.
 */

public class Machine {
	
	public enum MachineType { fountain, fryer, grillPress, oven }
	
	// Converts Machine instances into strings based on MachineType.
	
	public String toString() {
		switch (machineType) {
		case fountain: 		return "Fountain";
		case fryer:			return "Fryer";
		case grillPress:	return "Grill Press";
		case oven:			return "Oven";
		default:			return "INVALID MACHINE";
		}
	}
	
	final MachineType machineType;
	final Food machineFoodType;

	volatile int itemsCooking = 0;
    private final int capacity;

	Machine(MachineType machineType, Food food, int capacityIn) {
		this.machineType = machineType;
		this.machineFoodType = food;
		this.capacity = capacityIn;
	}

	synchronized boolean makeFood(Cook cook, int orderNum) throws InterruptedException {
	    if (itemsCooking < capacity) {
            itemsCooking++;
            Thread cookThread = new Thread(new CookAnItem(orderNum, this));
            Simulation.logEvent(SimulationEvent.cookStartedFood(cook, machineFoodType, orderNum));
            cookThread.start();
            return true;
        }
        return false;
	}

	private class CookAnItem implements Runnable {
	    private final int orderNum;
	    private final Machine machine;

	    CookAnItem(int orderNum, Machine machine) {
	        this.orderNum = orderNum;
	        this.machine = machine;
        }

		public void run() {
			try {
                Simulation.logEvent(SimulationEvent.machineCookingFood(machine, machineFoodType));
                Thread.sleep(machineFoodType.cookTimeS);
                Simulation.updateCookedOrder(machine, orderNum, machineFoodType);

			} catch(InterruptedException e) {
                System.out.println("Cooking thread interrupted.");
            }
		}
	}
}