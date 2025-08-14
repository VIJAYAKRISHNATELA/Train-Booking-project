package ticket.booking;

import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.services.UserBookingService;
import ticket.booking.util.UserServiceUtil;

import java.io.IOException;
import java.sql.Time;
import java.util.*;

public class App {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        System.out.println("Train Booking System");
        Scanner sc = new Scanner(System.in);
        int option = 0;
        UserBookingService userBookingService;
        Train trainSelectedForBooking = null;
        boolean isLoggedIn = false;
        String currentUser = null;

        // Store source and destination for booking
        String currentSource = null;
        String currentDestination = null;

        try {
            userBookingService = new UserBookingService();
        } catch(IOException ex) {
            System.out.println("Error initializing the application: " + ex.getMessage());
            return;
        }

        while(option != 7) {
            System.out.println("\nChoose Option");
            System.out.println("1. Sign Up");
            System.out.println("2. Login" + (isLoggedIn ? " (Currently logged in as: " + currentUser + ")" : ""));
            System.out.println("3. Fetch Bookings");
            System.out.println("4. Search Trains");
            System.out.println("5. Book a Seat" + (trainSelectedForBooking != null ?
                    " for " + trainSelectedForBooking.getTrainId() +
                            " (" + currentSource + " to " + currentDestination + ")" : ""));
            System.out.println("6. Cancel My Booking");
            System.out.println("7. Exit the Application");
            System.out.println("8. for debugging");
            try {
                option = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number (1-7).");
                continue;
            }
            switch (option) {
                case 1:
                    System.out.println("Enter the Username to SignUp:");
                    String nameToSignUp = sc.nextLine().trim();
                    if (nameToSignUp.isEmpty()) {
                        System.out.println("Username cannot be empty. Please try again.");
                        break;
                    }

                    System.out.println("Enter the password to SignUp:");
                    String passwordToSignUp = sc.nextLine().trim();
                    if (passwordToSignUp.length() < 6) {
                        System.out.println("Password must be at least 6 characters. Please try again.");
                        break;
                    }

                    User userToSignUp = new User(
                            nameToSignUp,
                            passwordToSignUp,
                            UserServiceUtil.hashPassword(passwordToSignUp),
                            new ArrayList<>(),
                            UUID.randomUUID().toString()
                    );

                    boolean signupSuccess = userBookingService.signUp(userToSignUp);
                    if (signupSuccess) {
                        System.out.println("Successfully signed up! You can now login.");
                    } else {
                        System.out.println("Username already exists or signup failed. Please try again.");
                    }
                    break;

                case 2:
                    System.out.println("Enter the username to Login:");
                    String nameToLogin = sc.nextLine().trim();
                    System.out.println("Enter the password to SignIn:");
                    String passwordToLogin = sc.nextLine().trim();

                    User userToLogin = new User(
                            nameToLogin,
                            passwordToLogin,
                            UserServiceUtil.hashPassword(passwordToLogin),
                            new ArrayList<>(),
                            UUID.randomUUID().toString()
                    );

                    try {
                        // Check if this user exists and password matches
                        boolean loginSuccess = userBookingService.login(userToLogin);
                        if (loginSuccess) {
                            isLoggedIn = true;
                            currentUser = nameToLogin;
                            System.out.println("Successfully logged in as " + nameToLogin);
                        } else {
                            System.out.println("Invalid username or password. Please try again.");
                        }
                    } catch(Exception ex) {
                        System.out.println("Error during login: " + ex.getMessage());
                    }
                    break;

                case 3:
                    if (!isLoggedIn) {
                        System.out.println("Please login first to fetch your bookings.");
                        break;
                    }

                    System.out.println("Fetching your Bookings...");
                    userBookingService.fetchBooking();
                    break;

                case 4:
                    System.out.println("Type your source Station:");
                    currentSource = sc.nextLine().trim();
                    System.out.println("Type your Destination Station:");
                    currentDestination = sc.nextLine().trim();

                    List<Train> trains = userBookingService.getTrains(currentSource, currentDestination);

                    if (trains.isEmpty()) {
                        System.out.println("No trains found for this route. Please try different stations.");
                        break;
                    }

                    int index = 1;
                    for(Train t : trains) {
                        System.out.println(index + ". Train ID: " + t.getTrainId());
                        for(Map.Entry<String, Time> entry : t.getStationTimes().entrySet()) {
                            System.out.println("   Station " + entry.getKey() + " time: " + entry.getValue());
                        }
                        index++;
                    }

                    System.out.println("Select a train by typing 1,2,3... (0 to cancel):");
                    try {
                        int selection = Integer.parseInt(sc.nextLine());
                        if (selection > 0 && selection <= trains.size()) {
                            trainSelectedForBooking = trains.get(selection - 1);
                            System.out.println("Selected train: " + trainSelectedForBooking.getTrainId());
                            System.out.println("You can now book a seat (Option 5)");
                        } else if (selection != 0) {
                            System.out.println("Invalid selection. Please try again.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid number.");
                    }
                    break;

                case 5:
                    if (!isLoggedIn) {
                        System.out.println("Please login first to book a seat.");
                        break;
                    }

                    if (trainSelectedForBooking == null) {
                        System.out.println("Please search and select a train first (Option 4).");
                        break;
                    }

                    System.out.println("Booking for train: " + trainSelectedForBooking.getTrainId());
                    System.out.println("Journey: " + currentSource + " to " + currentDestination);

                    System.out.println("Select a seat from below...");
                    List<List<Integer>> seats = userBookingService.fetchSeats(trainSelectedForBooking);

                    // Better seat display format
                    System.out.println("Seat Map (0=available, 1=booked):");
                    int rowNum = 0;
                    for(List<Integer> r : seats) {
                        System.out.print("Row " + rowNum + ": ");
                        for(Integer val : r) {
                            System.out.print(val + " ");
                        }
                        System.out.println();
                        rowNum++;
                    }

                    System.out.println("Select the seat that is available");
                    System.out.println("Enter the row:");
                    int row;
                    try {
                        row = Integer.parseInt(sc.nextLine());
                        if (row < 0 || row >= seats.size()) {
                            System.out.println("Invalid row. Please try again.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid row number.");
                        break;
                    }

                    System.out.println("Enter the column:");
                    int col;
                    try {
                        col = Integer.parseInt(sc.nextLine());
                        if (col < 0 || col >= seats.get(0).size()) {
                            System.out.println("Invalid column. Please try again.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid column number.");
                        break;
                    }

                    Boolean booked = userBookingService.bookTrainSeat(
                            trainSelectedForBooking,
                            row,
                            col,
                            currentSource,
                            currentDestination
                    );

                    if (booked) {
                        System.out.println("Booking successful! Enjoy your journey!");
                    } else {
                        System.out.println("Couldn't book the seat. It might be already taken or invalid.");
                    }
                    break;

                case 6:
                    if (!isLoggedIn) {
                        System.out.println("Please login first to cancel a booking.");
                        break;
                    }

                    System.out.println("Enter the ticket ID you want to cancel:");
                    String ticketId = sc.nextLine().trim();

                    Boolean cancelled = userBookingService.cancelBooking(ticketId);
                    if (cancelled) {
                        System.out.println("Your ticket has been cancelled successfully.");
                    } else {
                        System.out.println("Could not cancel the ticket. Please check the ID and try again.");
                    }
                    break;
                case 7:
                    System.out.println("Thank you for using the Train Booking System. Goodbye!");
                    break;
                case 8: // Temporary debug option
                    System.out.println("Running debug...");
                    userBookingService.debugTrainData();
                    break;

                default:
                    System.out.println("Invalid option. Please select a number between 1 and 7.");
                    break;
            }
        }
        sc.close();
    }
}