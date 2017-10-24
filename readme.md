# Multi-threaded Restaurant Simulation

A class project that simulates a day of operation for a restaurant. Simulation is run using multiple threads and without the use of built in concurrent Java libraries.

Simulation includes:

- Customers who line up to enter the restaurant.
- Restaurant only allows a max capacity in at a time.
- Customers who have entered place an order.
- Orders are sent to chefs. An available chef will take an order and begin cooking.
- Cooking is done by dedicated machines that can cook a type of food. Each machine has a specified time it takes to cook a food item.
- Machines can only cook a specified amount of food at a time. If capacity of machine is full, cook must wait before being able to use that machine. Cook will attempt to use other machines if needed while waiting.
- When all items in an order are completed, the customer who placed order initially will receive their food and leave the restaurant, allowing another waiting customer to enter.

Customers, chefs, and machines run on their individual threads.

Simulation parameters can be put in from the command line or in the main() function of Simulation.java. The number of customers, restaurant capacity, number of chefs, and machine capacities can be set. Project includes a Validate.java that runs multiple tests of the output log of the simulation.
