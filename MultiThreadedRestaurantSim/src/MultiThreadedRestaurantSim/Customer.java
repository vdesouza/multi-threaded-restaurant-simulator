package MultiThreadedRestaurantSim;

import java.util.ArrayList;
import java.util.List;

/**
 * Customers are simulation actors that have two fields: a name, and a list
 * of Food items that constitute the Customer's order.  When running, an
 * customer attempts to enter the restaurant (only successful if the
 * restaurant has a free table), place its order, and then leave the
 * restaurant when the order is complete.
 */
public class Customer implements Runnable {
	private final String name;
	private final List<Food> order;
	private final int orderNum;    
	
	private static int runningCounter = 0;

	Customer(String name, List<Food> order) {
		this.name = name;
		this.order = new ArrayList<>();
		this.order.addAll(order);
		this.orderNum = ++runningCounter;
	}

	public String toString() {
		return name;
	}

	/** 
	 * The customer attempts to enter the restaurant
	 * (only successful when the restaurant has a free table),
	 * place its order, and then leave the restaurant
	 * when the order is complete.
	 */
	public void run() {
        Simulation.logEvent(SimulationEvent.customerStarting(this));
        boolean entered = false;
        while (!entered) {
            entered = Simulation.enterRestaurant();
        }
        Simulation.logEvent(SimulationEvent.customerEnteredRestaurant(this));


        // Customer orders
        Simulation.logEvent(SimulationEvent.customerPlacedOrder(this, new ArrayList<Food>(this.order), this.orderNum));
        Simulation.placeOrder(this.orderNum, this.order);

        // Customer waits for order
        boolean customerWaiting = true;
        while (customerWaiting) {
            if (Simulation.checkOrderStatus(this.orderNum)) {
                // Customer leaves restaurant
                customerWaiting = false;
                List<Food> orderComplete = Simulation.getCompletedOrder(this.orderNum);
                if (orderComplete == null) {
                    Simulation.logEvent(SimulationEvent.customerReceivedOrder(this, new ArrayList<Food>(), orderNum));
                } else {
                    Simulation.logEvent(SimulationEvent.customerReceivedOrder(this, orderComplete, orderNum));
                }
                Simulation.logEvent(SimulationEvent.customerLeavingRestaurant(this));
                Simulation.leaveRestaurant();
            }
        }
	}
}