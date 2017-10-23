package MultiThreadedRestaurantSim;

import java.util.*;

public class Simulation {
	// List to track simulation events during simulation
    private static List<SimulationEvent> events;

    synchronized static void logEvent(SimulationEvent event) {
		events.add(event);
		System.out.println(event);
	}

    static HashMap<String, Machine> machines;
    private static int tables;

    // an instanceLock object for synchronizing on entering and leaving customers
    private static final Object frontDoor = new Object();
    private static volatile int numCustomers = 0;
    static boolean enterRestaurant() {
        synchronized (frontDoor) {
            if (numCustomers < tables) {
                numCustomers++;
                return true;
            } else {
                return false;
            }
        }
    }
    static void leaveRestaurant() {
        synchronized (frontDoor) {
            numCustomers--;
        }
    }

    // an instanceLock object for synchronizing on placing and removing items. This is like the waiter in a restaurant.
    private static final Object orderLock = new Object();
    private static LinkedList<Integer> orderList = new LinkedList<Integer>();
    private static HashMap<Integer, List<Food>> ordersPlaced = new HashMap<Integer, List<Food>>();
    private static HashMap<Cook, List<Food>> cookOrderClaim = new HashMap<Cook, List<Food>>();
    private static HashMap<Cook, Integer> cookOrderNumClaim = new HashMap<Cook, Integer>();
    static void placeOrder(int orderNum, List<Food> order) {
        synchronized (orderLock) {
            orderList.add(orderNum);
            ordersPlaced.put(orderNum, order);
        }
    }
    static boolean orderAvailable(Cook cook) {
        synchronized (orderLock) {
            if (!ordersPlaced.isEmpty()) {
                int orderNum = orderList.pop();
                cookOrderClaim.put(cook, ordersPlaced.remove(orderNum));
                cookOrderNumClaim.put(cook, orderNum);
                return true;
            } else {
                return false;
            }
        }
    }
    static int cookGetOrderNum(Cook cook) {
        synchronized (orderLock) {
            return cookOrderNumClaim.remove(cook);
        }
    }
    static List<Food> cookGetOrder(Cook cook) {
        synchronized (orderLock) {
            return cookOrderClaim.remove(cook);
        }
    }

    private static final Object customerLock = new Object();
    private static List<Integer> completedOrders = new ArrayList<Integer>();
    static void orderCompleted(Cook cook, int orderNum) {
        synchronized (customerLock) {
            completedOrders.add(orderNum);
            Simulation.logEvent(SimulationEvent.cookCompletedOrder(cook, orderNum));
        }
    }
    static boolean checkOrderStatus(int orderNum) {
        synchronized (customerLock) {
            return completedOrders.contains(orderNum);
        }
    }

    // an instanceLock object for synchronizing on updating cooked items.
    private static final Object cookingLock = new Object();
    private static HashMap<Integer, List<Food>> ordersCooked = new HashMap<Integer, List<Food>>();
    private static HashMap<Integer, List<Food>> checkedOrdersCooked = new HashMap<Integer, List<Food>>();
    static void updateCookedOrder(Machine machine, int orderNum, Food foodCooked) {
        synchronized (cookingLock) {
            if (ordersCooked.containsKey(orderNum)) {
                ordersCooked.get(orderNum).add(foodCooked);
            } else {
                List<Food> cookedFoodList = new ArrayList<Food>();
                cookedFoodList.add(foodCooked);
                ordersCooked.put(orderNum, cookedFoodList);
            }
            logEvent(SimulationEvent.machineDoneFood(machine, foodCooked));
        }
    }

    static boolean checkCookingStatus(Cook cook, int orderNum, Food food) {
        synchronized (cookingLock) {
            if (ordersCooked.containsKey(orderNum)) {
                if(ordersCooked.get(orderNum).contains(food)) {
                    ordersCooked.get(orderNum).remove(food);
                    if (checkedOrdersCooked.containsKey(orderNum)) {
                        checkedOrdersCooked.get(orderNum).add(food);
                    } else {
                        List<Food> cookedFoodList = new ArrayList<Food>();
                        cookedFoodList.add(food);
                        checkedOrdersCooked.put(orderNum, cookedFoodList);
                    }
                    logEvent(SimulationEvent.cookFinishedFood(cook, food, orderNum));
                    machines.get(food.name).itemsCooking--;
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }

        }
    }
    static List<Food> getCompletedOrder(int orderNum) {
        synchronized (cookingLock) {
            return checkedOrdersCooked.remove(orderNum);
        }
    }

	public static List<SimulationEvent> runSimulation( int numCustomers, int numCooks, int numTables, int machineCapacity, boolean randomOrders) {

		// This method's signature MUST NOT CHANGE.

        tables = numTables;


		// We are providing this events list object for you.  
		// It is the ONLY PLACE where a concurrent collection object is 
		// allowed to be used.
		events = Collections.synchronizedList(new ArrayList<SimulationEvent>());


		// Start the simulation
		logEvent(SimulationEvent.startSimulation(numCustomers,
				numCooks,
				numTables,
				machineCapacity));


		// Start up machines
        machines = new HashMap<String, Machine>();
		machines.put(FoodType.wings.name, new Machine(Machine.MachineType.fryer, FoodType.wings, machineCapacity));
        logEvent(SimulationEvent.machineStarting(machines.get(FoodType.wings.name), FoodType.wings, machineCapacity));
        machines.put(FoodType.pizza.name, new Machine(Machine.MachineType.oven, FoodType.pizza, machineCapacity));
        logEvent(SimulationEvent.machineStarting(machines.get(FoodType.pizza.name), FoodType.pizza, machineCapacity));
        machines.put(FoodType.sub.name, new Machine(Machine.MachineType.grillPress, FoodType.sub, machineCapacity));
        logEvent(SimulationEvent.machineStarting(machines.get(FoodType.sub.name), FoodType.sub, machineCapacity));
        machines.put(FoodType.soda.name, new Machine(Machine.MachineType.fountain, FoodType.soda, machineCapacity));
        logEvent(SimulationEvent.machineStarting(machines.get(FoodType.soda.name), FoodType.soda, machineCapacity));

		// Let cooks in
        Thread[] cooks = new Thread[numCooks];
        for (int i = 0; i < numCooks; i++) {
            cooks[i] = new Thread(
                    new Cook("Cook " + i)

            );
        }
        for (int i = 0; i < numCooks; i++) {
            cooks[i].start();
        }


		// Build the customers.
		Thread[] customers = new Thread[numCustomers];
		LinkedList<Food> order;
		if (!randomOrders) {
			order = new LinkedList<Food>();
			order.add(FoodType.wings);
			order.add(FoodType.pizza);
			order.add(FoodType.sub);
			order.add(FoodType.soda);
			for(int i = 0; i < customers.length; i++) {
				customers[i] = new Thread(
						new Customer("Customer " + (i), order)
						);
			}
		}
		else {
			for(int i = 0; i < customers.length; i++) {
				Random rnd = new Random();
				int wingsCount = rnd.nextInt(4);
				int pizzaCount = rnd.nextInt(4);
				int subCount = rnd.nextInt(4);
				int sodaCount = rnd.nextInt(4);
				order = new LinkedList<Food>();
				for (int b = 0; b < wingsCount; b++) {
					order.add(FoodType.wings);
				}
				for (int f = 0; f < pizzaCount; f++) {
					order.add(FoodType.pizza);
				}
				for (int f = 0; f < subCount; f++) {
					order.add(FoodType.sub);
				}
				for (int c = 0; c < sodaCount; c++) {
					order.add(FoodType.soda);
				}
				customers[i] = new Thread(
						new Customer("Customer " + (i), order)
				);
			}
		}

		for(int i = 0; i < customers.length; i++) {
			customers[i].start();
		}


		try {
			// Wait for customers to finish
			for (int i = 0; i < customers.length; i++) {
			    customers[i].join();
            }


			for(int i = 0; i < cooks.length; i++) {
			    cooks[i].interrupt();
            }
            for(int i = 0; i < cooks.length; i++) {
                cooks[i].join();
            }

		}
		catch(InterruptedException e) {
			System.out.println("Simulation thread interrupted.");
		}

		// Shut down machines
        Set<String> machinesList = machines.keySet();
        for (String machine : machinesList) {
            logEvent(SimulationEvent.machineEnding(machines.get(machine)));
        }


		// Done with simulation		
		logEvent(SimulationEvent.endSimulation());

		return events;
	}

	/**
	 * Entry point for the simulation.
	 */
	public static void main(String args[]) throws InterruptedException {
		// Parameters to the simulation
		/*
		if (args.length != 4) {
			System.err.println("usage: java Simulation <#customers> <#cooks> <#tables> <capacity> <randomorders");
			System.exit(1);
		}
		int numCustomers = new Integer(args[0]).intValue();
		int numCooks = new Integer(args[1]).intValue();
		int numTables = new Integer(args[2]).intValue();
		int machineCapacity = new Integer(args[3]).intValue();
		boolean randomOrders = new Boolean(args[4]);
		 */

		//int numCustomers = 40;
		//int numCooks = 8;
		//int numTables = 12;
		//int machineCapacity = 4;
		//boolean randomOrders = true;

        Random rnd = new Random();
        int numCustomers = rnd.nextInt(1000);
        int numCooks = rnd.nextInt(100);
        int numTables = rnd.nextInt(50);
        int machineCapacity = rnd.nextInt(20);
        boolean randomOrders = true;

		// Run the simulation and then 
		//   feed the result into the method to validate simulation.
		System.out.println("Did it work? " +
				Validate.validateSimulation(
						runSimulation(
								numCustomers, numCooks,
								numTables, machineCapacity,
								randomOrders
								)
						)
				);
	}

}



