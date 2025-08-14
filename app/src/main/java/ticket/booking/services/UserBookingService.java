package ticket.booking.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.util.AppConfig;
import ticket.booking.util.UserServiceUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class UserBookingService {
    private List<User> userList;
    private User user;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public UserBookingService() throws IOException {
        loadUsers();
    }
    public void debugTrainData() {
        try {
            // Try to load trains data directly to check if it's available
            File trainsFile = new File(AppConfig.TRAINS_FILE_PATH);
            if (trainsFile.exists()) {
                List<Train> trains = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
                System.out.println("Found " + trains.size() + " trains in file.");

                for (Train t : trains) {
                    System.out.println("Train: " + t.getTrainId());
                    System.out.println("  Stations: " + t.getStations());
                }
            } else {
                System.out.println("Trains file not found at: " + AppConfig.TRAINS_FILE_PATH);
            }
        } catch (Exception e) {
            System.out.println("Error reading trains file: " + e.getMessage());
        }
    }

    private void loadUsers() throws IOException {
        // Create directory if it doesn't exist
        File directory = new File(AppConfig.DATA_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Try to load from external file
        File usersFile = new File(AppConfig.USERS_FILE_PATH);
        if (usersFile.exists()) {
            userList = objectMapper.readValue(usersFile, new TypeReference<List<User>>() {});
        } else {
            // If external file doesn't exist, try to load from resources
            InputStream is = getClass().getResourceAsStream("/localDb/users.json");
            if (is != null) {
                userList = objectMapper.readValue(is, new TypeReference<List<User>>() {});
                // Save to external location for future use
                saveUserList();
            } else {
                // Start with empty list if no file exists
                userList = new ArrayList<>();
            }
        }
    }

    private void saveUserList() {
        try {
            File directory = new File(AppConfig.DATA_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            objectMapper.writeValue(new File(AppConfig.USERS_FILE_PATH), userList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Boolean login(User userToLogin) {
        // Find user with matching username and password
        Optional<User> foundUser = userList.stream()
                .filter(existingUser ->
                        existingUser.getName().equalsIgnoreCase(userToLogin.getName()) &&
                        UserServiceUtil.checkPassword(userToLogin.getPassword(), existingUser.getHashedPassword())
                ).findFirst();
        // If user found, set as current user
        if (foundUser.isPresent()) {
            this.user = foundUser.get(); // Set the current user to the found user
            return true;
        }
        return false;
    }
    public Boolean signUp(User user1){
        userList.add(user1);
        saveUserList();
        return Boolean.TRUE;
    }

    public void fetchBooking(){
        Optional<User> userFetched = userList.stream().filter(u1 ->
                u1.getName().equals(user.getName()) &&
                        UserServiceUtil.checkPassword(user.getPassword(), u1.getHashedPassword())
        ).findFirst();

        if (userFetched.isPresent()) {
            User currentUser = userFetched.get();
            List<Ticket> tickets = currentUser.getTicketsBooked();

            if (tickets == null || tickets.isEmpty()) {
                System.out.println("No bookings found for this user.");
                return;
            }

            System.out.println("\n==== YOUR BOOKINGS ====");
            int count = 1;
            for (Ticket ticket : tickets) {
                System.out.println(count + ". Ticket ID: " + ticket.getTicketId());
                System.out.println("   From: " + ticket.getSource() + " To: " + ticket.getDestination());
                System.out.println("   Date: " + ticket.getDateOfTravel());
                System.out.println("   Train: " + ticket.getTrain().getTrainId());
                System.out.println("   --------------------------");
                count++;
            }
        } else {
            System.out.println("User not found or password incorrect.");
        }
    }
    public Boolean cancelBooking(String ticketId){
        if(ticketId == null || ticketId.isEmpty()){
            System.out.println("Ticket ID can't be Empty or Null");
            return false;
        }
        String finalTicketId1 = ticketId;
        boolean removed = user.getTicketsBooked().removeIf(ticket -> ticket.getTicketId().equals(finalTicketId1));
        String finalTicketId = ticketId;
        user.getTicketsBooked().removeIf(Ticket -> Ticket.getTicketId().equals(finalTicketId));
        if(removed){
            System.out.println("Ticke with ID : " + ticketId + " has been cancelled.");
            return Boolean.TRUE;
        }else{
            System.out.println("No ticket found with ID : " + ticketId);
            return Boolean.FALSE;
        }
    }

    public List<Train> getTrains(String source, String destination) {
        System.out.println("Searching for trains from " + source + " to " + destination);

        try {
            // Load trains directly from file each time to ensure fresh data
            File trainsFile = new File(AppConfig.TRAINS_FILE_PATH);
            List<Train> allTrains = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
            System.out.println("Loaded " + allTrains.size() + " trains from file");

            List<Train> matchingTrains = new ArrayList<>();

            for (Train train : allTrains) {
                List<String> stations = train.getStations();
                System.out.println("Checking train " + train.getTrainId() + " with stations: " + stations);

                // Check if both source and destination are in this train's stations
                boolean hasSource = false;
                boolean hasDestination = false;
                int sourceIndex = -1;
                int destIndex = -1;

                // Find station indexes
                for (int i = 0; i < stations.size(); i++) {
                    String station = stations.get(i).toLowerCase();
                    String srcLower = source.toLowerCase();
                    String destLower = destination.toLowerCase();

                    if (station.equals(srcLower)) {
                        hasSource = true;
                        sourceIndex = i;
                    }
                    if (station.equals(destLower)) {
                        hasDestination = true;
                        destIndex = i;
                    }
                }

                // Only add if both stations exist AND source comes before destination
                if (hasSource && hasDestination && sourceIndex < destIndex) {
                    System.out.println("✅ Train " + train.getTrainId() + " matches criteria");
                    matchingTrains.add(train);
                } else {
                    System.out.println(" Train " + train.getTrainId() + " does not match criteria");
                    System.out.println("   Has source: " + hasSource + ", Has destination: " + hasDestination);
                    if (hasSource && hasDestination) {
                        System.out.println("   Source index: " + sourceIndex + ", Destination index: " + destIndex);
                    }
                }
            }

            System.out.println("Found " + matchingTrains.size() + " matching trains");
            return matchingTrains;
        } catch (Exception e) {
            System.out.println("Error finding trains: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    public List<List<Integer>> fetchSeats(Train train){
        return train.getSeats();
    }
    public boolean bookTrainSeat(Train train, int r, int seat, String source, String destination) {
        try {
            // Add debug statements to trace execution
            System.out.println("Attempting to book seat (" + r + "," + seat + ") on train " + train.getTrainId());

            // Create fresh TrainService to ensure we have latest data
            TrainService trainService = new TrainService();

            // We'll use the train directly since we don't have getAllTrains()
            // Get seats from the provided train
            List<List<Integer>> seats = train.getSeats();
            System.out.println("Current seat status: " + seats.get(r).get(seat));

            // Validate seat coordinates and availability
            if (r >= 0 && r < seats.size() && seat >= 0 && seat < seats.get(r).size()) {
                if (seats.get(r).get(seat) == 0) {
                    // Mark seat as booked
                    seats.get(r).set(seat, 1);
                    train.setSeats(seats);

                    // Update train in database - make sure this method exists
                    trainService.updateTrain(train);
                    System.out.println("Train updated in database");

                    // Create a new ticket
                    String ticketId = UUID.randomUUID().toString();
                    Ticket newTicket = new Ticket(
                            ticketId,
                            user.getUserId(),
                            source,
                            destination,
                            new Date(),
                            train
                    );

                    // Add ticket to user's bookings
                    if (user.getTicketsBooked() == null) {
                        user.setTicketsBooked(new ArrayList<>());
                    }
                    user.getTicketsBooked().add(newTicket);
                    System.out.println("Ticket added to user's bookings: " + ticketId);

                    // Find and update the current user in the userList
                    boolean userUpdated = false;
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getUserId().equals(user.getUserId())) {
                            userList.set(i, user);
                            userUpdated = true;
                            break;
                        }
                    }

                    if (!userUpdated) {
                        System.out.println("Warning: Couldn't find user in userList!");
                        return false;
                    }

                    // Save the updated user list
                    saveUserList();
                    System.out.println("User list saved with new booking");

                    return true;
                } else {
                    System.out.println("Seat is already booked!");
                    return false;
                }
            } else {
                System.out.println("Invalid seat coordinates!");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error booking seat: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
