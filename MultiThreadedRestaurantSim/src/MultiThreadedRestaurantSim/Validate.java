package MultiThreadedRestaurantSim;

import MultiThreadedRestaurantSim.SimulationEvent.EventType;

import java.util.*;


/**
 * Just a bunch of tests that validate a simulation run
 */
public class Validate {
    private static class InvalidSimulationException extends Exception {
        public InvalidSimulationException() { }
    }

    // Helper method for validating the simulation
    private static void check(boolean check, String message) throws InvalidSimulationException {
        if (!check) {
            System.err.println("SIMULATION INVALID : " + message);
            throw new Validate.InvalidSimulationException();
        }
    }

    private static final Food[] foodArr = { FoodType.wings, FoodType.pizza, FoodType.sub, FoodType.soda };

    private static HashMap<String, EventType> customerStates;
    private static HashMap<String, EventType> cookStates;
    private static HashMap<String, EventType> machineStates;

    private static String stateErrorMsg(String name, EventType oldEvent, EventType newEvent) {
        return name + " tried to switch from " + oldEvent + " to " + newEvent;
    }

    private static void updateCustomerState(String name, EventType newEvent) throws InvalidSimulationException {
        // Checks if the customer's new state logically follows from their previous state
        EventType oldEvent = customerStates.get(name);
        switch(newEvent) {
            case CustomerStarting:
                check(oldEvent == null, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CustomerEnteredRestaurant:
                check(oldEvent == EventType.CustomerStarting, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CustomerPlacedOrder:
                check(oldEvent == EventType.CustomerEnteredRestaurant, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CustomerReceivedOrder:
                check(oldEvent == EventType.CustomerPlacedOrder, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CustomerLeavingRestaurant:
                check(oldEvent == EventType.CustomerReceivedOrder, stateErrorMsg(name, oldEvent, newEvent));
                break;
            default:
                check(false, "Illegal customer state");
        }

        // Update customer state
        customerStates.put(name, newEvent);
    }

    private static void updateCookState(String name, EventType newEvent) throws InvalidSimulationException {
        // Check if the cook's new state logically follows from their previous state
        EventType oldEvent = cookStates.get(name);
        switch(newEvent) {
            case CookStarting:
                check(oldEvent == null, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CookReceivedOrder:
                check(oldEvent != EventType.CookEnding, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CookStartedFood:
                check(oldEvent != EventType.CookEnding, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CookFinishedFood:
                check(oldEvent != EventType.CookEnding &&
                                oldEvent != EventType.CookStarting,
                        stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CookCompletedOrder:
                check(oldEvent != EventType.CookEnding, stateErrorMsg(name, oldEvent, newEvent));
                break;
            case CookEnding:
                check(oldEvent == EventType.CookStarting ||
                                oldEvent == EventType.CookCompletedOrder,
                        stateErrorMsg(name, oldEvent, newEvent));
                break;
            default:
                check(false, "Illegal cook state");
        }

        // Update cook state
        cookStates.put(name, newEvent);
    }

    private static void updateMachineState(Machine.MachineType machType, EventType newEvent) throws InvalidSimulationException {
        // Check if the machine's new state logically follows from its previous state
        String name = machType.toString();
        EventType oldEvent = machineStates.get(name);
        switch(newEvent) {
            case MachineStarting:
                check(oldEvent == null, stateErrorMsg(name, oldEvent, newEvent));
                break;

            case MachineStartingFood:
                check(oldEvent != EventType.MachineEnding, stateErrorMsg(name, oldEvent, newEvent));
                break;

            case MachineDoneFood:
                check(oldEvent != EventType.MachineEnding &&
                                oldEvent != EventType.MachineStarting,
                        stateErrorMsg(name, oldEvent, newEvent));
                break;

            case MachineEnding:
                check(oldEvent == EventType.MachineStarting ||
                                oldEvent == EventType.MachineDoneFood,
                        stateErrorMsg(name, oldEvent, newEvent));
                break;

            default:
                check(false, "Illegal machine state");
        }

        //Update machine state
        machineStates.put(name, newEvent);
    }

    // Checks that the contents of order1 are the same as those of order2
    private static void sameOrderCheck(List<Food> order1, List<Food> order2, String errorMsg) throws InvalidSimulationException {
        HashMap<Food, Integer> foodCounts = new HashMap<Food, Integer>();
        for (Food item : foodArr)
            foodCounts.put(item,  0);

        // Increment for every item in order1, decrement for order2
        for (Food item : order1)
            foodCounts.put(item,  foodCounts.get(item) + 1);
        for (Food item : order2)
            foodCounts.put(item,  foodCounts.get(item) - 1);

        // If order1 and order2 contain the same elements, all counts should be 0
        for (Integer count : foodCounts.values())
            check(count == 0, errorMsg + "\n" + order1 + "\n" + order2);

    }

    /**
     * Validates the given list of events is a valid simulation.
     * Returns true if the simulation is valid, false otherwise.
     *
     * @param events - a list of events generated by the simulation
     *   in the order they were generated.
     *
     * @returns res - whether the simulation was valid or not
     */
    public static boolean validateSimulation(List<SimulationEvent> events) {
        try {
            check(events.get(0).event == EventType.SimulationStarting,
                    "Simulation didn't start with initiation event \n " +
                            events.get(0).event);
            check(events.get(events.size()-1).event ==
                            EventType.SimulationEnded,
                    "Simulation didn't end with termination event \n" +
                            events.get(events.size()-1).event);


            int numCustomers = events.get(0).simParams[0];
            int numTables = events.get(0).simParams[2];
            int numCooks = events.get(0).simParams[1];
            int customers = 0;
            int customersIn = 0;
            int cooksReported = 0;
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.CustomerEnteredRestaurant) {
                    customersIn++;
                } else if (e.event == EventType.CustomerLeavingRestaurant) {
                    customersIn--;
                } else if (e.event == EventType.CustomerStarting) {
                    customers++;
                } else if (e.event == EventType.CookStarting) {
                    cooksReported++;
                }
                //TEST: The Restaurant capacity should not be exceeded
                check(customersIn <= numTables, "More customers in Restaurant than tables.");
            }
            //TEST: Should not have more eaters than specified
            check(numCustomers == customers, "More eaters have shown up.");
            //TEST: Should not have more cooks than specified
            check(numCooks == cooksReported, "More or less cooks reported for work.");

            //TEST: The capacity of each machine should not be exceeded
            int machineCapacity = events.get(0).simParams[3];
            int fryerCurrCapacity = 0;
            int ovenCurrCapacity = 0;
            int grillCurrCapacity = 0;
            int fountainCurrCapacity = 0;
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.MachineStartingFood) {
                    Machine machine = e.machine;
                    if (machine.machineFoodType == FoodType.wings) {
                        fryerCurrCapacity++;
                        check(fryerCurrCapacity <= machineCapacity, "Fryer capacity is greater than machine");
                    } else if (machine.machineFoodType == FoodType.pizza) {
                        ovenCurrCapacity++;
                        check(ovenCurrCapacity <= machineCapacity, "Oven capacity is greater than machine");
                    } else if (machine.machineFoodType == FoodType.sub) {
                        grillCurrCapacity++;
                        check(grillCurrCapacity <= machineCapacity, "Grill capacity is greater than machine");
                    } else if (machine.machineFoodType == FoodType.soda) {
                        fountainCurrCapacity++;
                        check(fountainCurrCapacity <= machineCapacity, "Fountain capacity is greater than machine");
                    }
                } else if (e.event == EventType.MachineDoneFood) {
                    Machine machine = e.machine;
                    if (machine.machineFoodType == FoodType.wings) {
                        fryerCurrCapacity--;
                        check(fryerCurrCapacity <= machineCapacity, "Fryer capacity is greater than machine");
                    } else if (machine.machineFoodType == FoodType.pizza) {
                        ovenCurrCapacity--;
                        check(ovenCurrCapacity <= machineCapacity, "Oven capacity is greater than machine");
                    } else if (machine.machineFoodType == FoodType.sub) {
                        grillCurrCapacity--;
                        check(grillCurrCapacity <= machineCapacity, "Grill capacity is greater than machine");
                    } else if (machine.machineFoodType == FoodType.soda) {
                        fountainCurrCapacity--;
                        check(fountainCurrCapacity <= machineCapacity, "Fountain capacity is greater than machine");
                    }
                }
            }

            //TEST: Eater should not receive order until cook completes it
            ArrayList<Integer> currOrders = new ArrayList<>(events.get(0).simParams[0]);
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.CookCompletedOrder) {
                    currOrders.add(e.orderNumber);
                }
                if (e.event == EventType.CustomerReceivedOrder) {
                    check(currOrders.contains(e.orderNumber), "Customer received an order before cook completes it. \n" +
                            e.customer + " " + e.orderNumber);
                }
            }

            //TEST: Eater should not leave Restaurant until order is received
            ArrayList<Customer> currCustomers = new ArrayList<>(events.get(0).simParams[0]);
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.CustomerReceivedOrder) {
                    currCustomers.add(e.customer);
                }
                if (e.event == EventType.CustomerLeavingRestaurant) {
                    check(currCustomers.contains(e.customer), "Customer left before receiving an order. \n" +
                            e.customer);
                }
            }

            //TEST: Eater should not place more than one order
            ArrayList<Customer> currCustomersWhoHaveOrdered = new ArrayList<>(events.get(0).simParams[0]);
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.CustomerPlacedOrder) {
                    check(!currCustomersWhoHaveOrdered.contains(e.customer), "Customer is placing more than one orders. \n" +
                            e.customer);
                    currCustomersWhoHaveOrdered.add(e.customer);
                }
            }

            //TEST: Cook should not work on order before it is placed
            ArrayList<Integer> ordersPlaced = new ArrayList<>(events.get(0).simParams[0]);
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.CustomerPlacedOrder) {
                    ordersPlaced.add(e.orderNumber);
                }
                if (e.event == EventType.CookReceivedOrder) {
                    check(ordersPlaced.contains(e.orderNumber), "Cook started order before it was received. \n" +
                            e.cook + " " + e.orderNumber + "    " + e.toString());
                }
            }

            //TEST: Cook finishes all food in order before marking it done
            HashMap<Cook, List<Food>> cookList = new HashMap<>();
            HashMap<Cook, Integer> orderList = new HashMap<>();
            HashMap<Cook, List<Food>> cookingList = new HashMap<>();
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.CookReceivedOrder) {
                    cookList.put(e.cook, e.orderFood);
                    orderList.put(e.cook, e.orderNumber);
                } else if (e.event == EventType.CookFinishedFood) {
                    if (cookingList.containsKey(e.cook)) {
                        cookingList.get(e.cook).add(e.food);
                    } else {
                        ArrayList<Food> foodList = new ArrayList<>();
                        foodList.add(e.food);
                        cookingList.put(e.cook, foodList);
                    }
                } else if (e.event == EventType.CookCompletedOrder) {
                    check(e.orderNumber == orderList.get(e.cook), "Current cook orders do not match");
                    if (cookingList.get(e.cook) == null) {
                        cookingList.put(e.cook, new ArrayList<>());
                    }
                    check(equalLists(cookingList.get(e.cook), (cookList.get(e.cook))), "Finished cooking list does not match original \n" +
                            "cookingList = " + cookingList.get(e.cook) + " cookList = " + cookList.get(e.cook) + " ordernum = " + e.orderNumber);
                    cookList.remove(e.cook);
                    orderList.remove(e.cook);
                    cookingList.remove(e.cook);
                }
            }

            //TEST: Customer receives order that was placed
            HashMap<Customer, List<Food>> customerOriginalOrders = new HashMap<>();
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.CustomerPlacedOrder) {
                    customerOriginalOrders.put(e.customer, e.orderFood);
                }
                if (e.event == EventType.CustomerReceivedOrder) {
                    check(equalLists(e.orderFood, customerOriginalOrders.get(e.customer)), "Orginal and received orders do not match. \n" +
                            "Received: " + e.orderFood + " ordered: " + customerOriginalOrders.get(e.customer) + " " + e.orderNumber);
                }
            }


            //TEST: All machines are done cooking before machines are turned off
            ArrayList<Machine> doneMachines = new ArrayList<>();
            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                if (e.event == EventType.MachineEnding) {
                    doneMachines.add(e.machine);
                }
                if (e.event == EventType.MachineDoneFood) {
                    check(!doneMachines.contains(e.machine), "Machine ended before cooking was done \n" +
                            e);
                }
            }

            int[] simParams = events.get(0).simParams;
            int capacity = simParams[3];

            int numCustomersHandled = 0;
            int tableLoad = 0;

            // Records the current capacity of each machine
            HashMap<Food, Integer> foodLoad = new HashMap<Food, Integer>();

            // Keeps track of all orders that have ever been completed
            HashMap<Integer, Integer> completedOrders = new HashMap<Integer, Integer>();

            // Records the cook responsible for handling each order
            HashMap<Integer, Cook> orderHandlers = new HashMap<Integer, Cook>();

            // Records all items that have ever been cooked for each order
            HashMap<Integer, List<Food>> completedItems = new HashMap<Integer, List<Food>>();

            // Records all orders that customers have ever placed
            HashMap<Integer, List<Food>> allOrders = new HashMap<Integer, List<Food>>();

            customerStates = new HashMap<String, EventType>();
            cookStates = new HashMap<String, EventType>();
            machineStates = new HashMap<String, EventType>();

            for (Food item : foodArr)
                foodLoad.put(item,  0);

            for (int i = 0; i < events.size(); i++) {
                SimulationEvent e = events.get(i);
                switch(e.event) {
                    case SimulationStarting:
                        break;

                    case SimulationEnded:
                        break;

                    case CustomerStarting:
                        // Call makes sure that this is the customer's first state
                        updateCustomerState(e.customer.toString(), e.event);
                        break;

                    case CustomerEnteredRestaurant:
                        // Customer can only enter restaurant if there are enough tables
                        check(++tableLoad <= numTables, "More customers in Restaurant than tables permit");

                        // Call makes sure that the customer has started before they entered Restaurant
                        updateCustomerState(e.customer.toString(), e.event);
                        break;

                    case CustomerPlacedOrder:
                        // Customer can only place order if it hasn't been placed before
                        check(allOrders.get(e.orderNumber) == null, "Order number has already been placed");
                        allOrders.put(e.orderNumber, e.orderFood);

                        // Call makes sure that customer enters Restaurant before they place order
                        updateCustomerState(e.customer.toString(), e.event);
                        break;

                    case CustomerReceivedOrder:
                        // Customer cannot their order before it is complete
                        check(completedOrders.get(e.orderNumber) != null, "Customer received order before it was complete");

                        // Call makes sure that the customer has placed their order before receiving it
                        updateCustomerState(e.customer.toString(), e.event);
                        break;

                    case CustomerLeavingRestaurant:
                        // There cannot be fewer than 0 customers in the restaurant
                        check(--tableLoad >= 0, "Number of customers in Restaurant is negative");

                        // Call makes sure that customer has received their order before they leave
                        updateCustomerState(e.customer.toString(), e.event);
                        numCustomersHandled++;
                        break;

                    case CookStarting:
                        // Call makes sure that this is the cook's first state
                        updateCookState(e.cook.toString(), e.event);
                        break;

                    case CookReceivedOrder:
                        // Cook cannot receive an order that was never placed by a customer
                        check(allOrders.get(e.orderNumber) != null, e.cook + " received order that was never placed");

                        // Cook cannot receive an order that is in progress or was completed
                        check(orderHandlers.get(e.orderNumber) == null, e.cook + " received order that was already given to " + orderHandlers.get(e.orderNumber));
                        completedItems.put(e.orderNumber, new LinkedList<Food>());
                        orderHandlers.put(e.orderNumber, e.cook);

                        // Cook's received order must be equivalent to customer's placed order
                        sameOrderCheck(e.orderFood,
                                allOrders.get(e.orderNumber),
                                "Mismatch between cook's received order and customer's placed order");

                        // Call makes sure that cook has not ended yet
                        updateCookState(e.cook.toString(), e.event);
                        break;

                    case CookStartedFood:
                        // Cook cannot exceed the machine's capacity
                        check(foodLoad.get(e.food) < capacity, "Machine holding too much of " + e.food + "\n" + e + "\n" + foodLoad.get(e.food));
                        foodLoad.put(e.food, foodLoad.get(e.food) + 1);

                        // Cook cannot handle food for an order that they did not start
                        check(orderHandlers.get(e.orderNumber) == e.cook, e.cook + " starting food for order that was started by " + orderHandlers.get(e.orderNumber));

                        // Cook cannot start food for an order that is already complete
                        check(completedOrders.get(e.orderNumber) == null, e.cook + " started food for an order that was already complete");

                        // Call makes sure cook has not ended yet
                        updateCookState(e.cook.toString(), e.event);
                        break;

                    case CookFinishedFood:
                        // Cook cannot bring machine's load below zero
                        check(foodLoad.get(e.food) > 0, "Machine holding negative of " + e.food);
                        foodLoad.put(e.food, foodLoad.get(e.food) - 1);

                        // Cook cannot handle food for an order that they did not start
                        check(orderHandlers.get(e.orderNumber) == e.cook, e.cook + " finished food for order that was started by " + orderHandlers.get(e.orderNumber));

                        // Cook cannot retrieve food for an order that is already complete
                        check(completedOrders.get(e.orderNumber) == null, e.cook + " finished food for an order that was already complete");

                        completedItems.get(e.orderNumber).add(e.food);
                        updateCookState(e.cook.toString(), e.event);
                        break;

                    case CookCompletedOrder:
                        // Cook cannot complete order for an order that was not started by them;
                        check(orderHandlers.get(e.orderNumber) == e.cook, e.cook + " completed order that was started by " + orderHandlers.get(e.orderNumber));

                        // Cook cannot complete an order that is already complete
                        check(completedOrders.get(e.orderNumber) == null, e.cook + " completed order that was already complete");

                        // Cook can only complete an order when the order's completed items matches with the customer's order
                        sameOrderCheck(completedItems.get(e.orderNumber),
                                allOrders.get(e.orderNumber),
                                "Mismatch between cook's completed order and customer's placed order");
                        completedOrders.put(e.orderNumber, e.orderNumber);

                        // Call makes sure the cook has not ended yet and that the cook did not just start working
                        updateCookState(e.cook.toString(), e.event);
                        break;

                    case CookEnding:
                        // Cooks cannot leave while there are still customers left to handle
                        check(numCustomersHandled == numCustomers, "Cook left before all the customers were handled");

                        // Call makes sure that the cook is not in the middle of an order
                        updateCookState(e.cook.toString(), e.event);
                        break;

                    case MachineStarting:
                        // Call makes sure that this is the machine's first state
                        updateMachineState(e.machine.machineType, e.event);
                        break;

                    case MachineStartingFood:
                        // NOTE: DOES NOT CHECK CAPACITY HERE. RELIES ON CAPACITY CHECKS ON THE COOKS' END

                        // Call makes sure that the machine has not ended yet
                        updateMachineState(e.machine.machineType, e.event);
                        break;

                    case MachineDoneFood:
                        // NOTE: DOES NOT CHECK CAPACITY HERE. RELIES ON CAPACITY CHECKS ON THE COOKS' END

                        // Call makes sure the machine has not ended and has not just starteds
                        updateMachineState(e.machine.machineType, e.event);
                        break;

                    case MachineEnding:
                        // Call makes sure that the machine has not ended yet and is not in the middle of cooking
                        updateMachineState(e.machine.machineType, e.event);
                        break;

                    default:
                        System.out.println("VALIDATION CODE DOES NOT HANDLE EVENT " + e.event);
                        throw new InvalidSimulationException();
                }
            }

            Collection<EventType> finalCustomerStates = customerStates.values();
            Collection<EventType> finalCookStates = cookStates.values();
            Collection<EventType> finalMachineStates = machineStates.values();

            // Check that the simulation contains events for the right number of customers, cooks, and machines
            check(finalCustomerStates.size() == numCustomers, "Simulation expected " + numCustomers + " customers, but log records " + finalCustomerStates.size());
            check(finalCookStates.size() == numCooks, "Simulation expected " + numCooks + " cooks, but log records " + finalCookStates.size());
            check(finalMachineStates.size() == 4, "Simulation expected 4 machines, but log records" + finalMachineStates.size());

            // Check the final state of each actor
            for (EventType e : finalCustomerStates)
                check(e == EventType.CustomerLeavingRestaurant, "At end of log, not all customers have left Restaurant");
            for (EventType e : finalCookStates)
                check(e == EventType.CookEnding, "At end of log, not all cooks have ended");
            for (EventType e : finalMachineStates)
                check(e == EventType.MachineEnding, "At end of log, not all machines have shut down");

            // Check that the number of completed orders is equal to the number of customers
            check(completedOrders.size() == numCustomers, "At end of log, number of completed orders does not match up with number of customers");


            System.out.println(events.get(0));
            return true;
        } catch (InvalidSimulationException e) {
            return false;
        }
    }

    public static boolean equalLists(List<Food> one, List<Food> two) {
        if (one == null && two == null){
            return true;
        }

        if((one == null && two != null)
                || one != null && two == null
                || one.size() != two.size()){
            return false;
        }

        ArrayList<String> one2 = new ArrayList<>();
        for (Food food : one) {
            one2.add(food.toString());
        }
        ArrayList<String> two2 = new ArrayList<>();
        for (Food food : two) {
            two2.add(food.toString());
        }

        Collections.sort(one2);
        Collections.sort(two2);
        return one2.equals(two2);
    }
}
