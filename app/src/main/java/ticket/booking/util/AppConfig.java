package ticket.booking.util;

/**
 * Contains application-wide constants and configuration values
 */
public class AppConfig {
    // File paths for data persistence
    public static final String DATA_DIR = "data";
    public static final String USERS_FILE_PATH = DATA_DIR + "/users.json";
    public static final String TRAINS_FILE_PATH = DATA_DIR + "/trains.json";

    // You can add other application-wide constants here
    public static final int MAX_SEATS_PER_BOOKING = 6;
    public static final int DEFAULT_BOOKING_EXPIRY_DAYS = 30;
    // etc.
}