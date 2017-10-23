package MultiThreadedRestaurantSim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cooks are simulation actors that have at least one field, a name.
 * When running, a cook attempts to retrieve outstanding orders placed
 * by Eaters and process them.
 */
public class Cook implements Runnable {
	private final String name;

	Cook(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	/**
	 * The cook tries to retrieve orders placed by Customers.
	 * For each order, a List<Food>, the cook submits each Food item in the
	 * List to an appropriate Machine, by calling makeFood().
	 * Once all machines have produced the desired Food, the order is complete,
	 * and the Customer is notified.  The cook can then go to process the next order.
	 * If during its execution the cook is interrupted (i.e., some
	 * other thread calls the interrupt() method on it, which could
	 * raise InterruptedException if the cook is blocking), then it terminates.
	 */
	public void run() {

		Simulation.logEvent(SimulationEvent.cookStarting(this));
        Random rnd = new Random();
		try {
			while(!Thread.interrupted()) {
                if (Simulation.orderAvailable(this)) {
                    int orderNum = Simulation.cookGetOrderNum(this);
                    List<Food> order = Simulation.cookGetOrder(this);
                    List<Food> rawFoodList = new ArrayList<>();
                    rawFoodList.addAll(order);
                    Simulation.logEvent(SimulationEvent.cookReceivedOrder(this, new ArrayList<>(order), orderNum));
                    while (rawFoodList.size() > 0) {
                        Food rawFood = rawFoodList.remove(rnd.nextInt(rawFoodList.size()));
                        if (!Simulation.machines.get(rawFood.name).makeFood(this, orderNum)) {
                            rawFoodList.add(rawFood);
                        }
                        // Checks that all completed items are done
                        // When cook sees an order item in the completed items, log as cookfinishedfood
                        Food food = order.remove(rnd.nextInt(order.size()));
                        if (!Simulation.checkCookingStatus(this, orderNum, food)) {
                            order.add(food);
                        }
                    }
                    while(order.size() > 0) {
                        Food food = order.remove(rnd.nextInt(order.size()));
                        if (!Simulation.checkCookingStatus(this, orderNum, food)) {
                            order.add(food);
                        }
                    }

                    // Update order num as complete to Sim
                    Simulation.orderCompleted(this, orderNum);
                }
			}
            Simulation.logEvent(SimulationEvent.cookEnding(this));
		}
		catch(InterruptedException e) {
			Simulation.logEvent(SimulationEvent.cookEnding(this));
		}
	}
}