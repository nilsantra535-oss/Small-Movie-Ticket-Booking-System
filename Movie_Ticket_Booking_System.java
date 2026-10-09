import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class User {
    private int userId;
    private String name;
    private String email;

    public User(int userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        if (isValidEmail(email)) {
            this.email = email;
        } else {
            this.email = "default@gmail.com";
        }
    }

    private boolean isValidEmail(String email) {
        return (email != null && email.contains("@"));
    }

    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}

class Customer extends User {
    public Customer(int userId, String name, String email) {
        super(userId, name, email);
    }
}

class Movie {
    private String movieId;
    private String title;
    private int duration;
    private String language;

    public Movie(String movieId, String title, int duration, String language) {
        this.movieId = movieId;
        this.title = title;
        this.duration = Math.max(duration, 0);
        this.language = language;
    }

    public String getTitle() { return title; }
    public int getDuration() { return duration; }
    public String getLanguage() { return language; }
}

class Theater {
    private String theaterId;
    private String name;
    private String location;

    public Theater(String theaterId, String name, String location) {
        this.theaterId = theaterId;
        this.name = name;
        this.location = location;
    }

    public String getName() { return name; }
    public String getLocation() { return location; }
}

class Seat {
    private String seatNumber;
    private boolean isBooked;

    public Seat(String seatNumber) {
        this.seatNumber = seatNumber;
        this.isBooked = false;
    }

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }
    public String getSeatNumber() { return seatNumber; }
}

class Show {
    private String showId;
    private Movie movie;
    private Theater theater;
    private List<Seat> seats;

    public Show(String showId, Movie movie, Theater theater) {
        this.showId = showId;
        this.movie = movie;
        this.theater = theater;
        this.seats = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            this.seats.add(new Seat("A" + i));
        }
    }

    public Movie getMovie() { return movie; }
    public Theater getTheater() { return theater; }
    public List<Seat> getSeats() { return seats; }

    public Seat getSeat(String seatNumber) {
        for (Seat seat : seats) {
            if (seat.getSeatNumber().equalsIgnoreCase(seatNumber)) {
                return seat;
            }
        }
        return null;
    }
}

class Booking {
    private String bookingId;
    private User user;
    private Show show;
    private Seat seat;

    public Booking(String bookingId, User user, Show show, Seat seat) {
        this.bookingId = bookingId;
        this.user = user;
        this.show = show;
        this.seat = seat;
    }

    public String getBookingDetails() {
        return "Booking Confirmed!\n" +
               "Booking ID: " + bookingId + "\n" +
               "User Name: " + user.getName() + "\n" +
               "User Email: " + user.getEmail() + "\n" +
               "Movie: " + show.getMovie().getTitle() + "\n" +
               "Theater: " + show.getTheater().getName() + " (" + show.getTheater().getLocation() + ")\n" +
               "Seat: " + seat.getSeatNumber();
    }
}

// Fixed Database Handler
class DatabaseHandler {
    private static final String URL = "jdbc:sqlite:movie_booking.db";

    static {
        try {
            // Register JDBC driver safely
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite Driver not found. Ensure sqlite-jdbc library is added.");
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        String createBookingsTable = "CREATE TABLE IF NOT EXISTS bookings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_name TEXT NOT NULL, " +
                "user_email TEXT NOT NULL, " +
                "movie_title TEXT NOT NULL, " +
                "seat_number TEXT NOT NULL" +
                ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createBookingsTable);
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    public static int saveBooking(String userName, String userEmail, String movieTitle, String seatNumber) {
        String insertSQL = "INSERT INTO bookings(user_name, user_email, movie_title, seat_number) VALUES(?, ?, ?, ?)";

        try (Connection conn = connect(); 
             PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, userName);
            pstmt.setString(2, userEmail);
            pstmt.setString(3, movieTitle);
            pstmt.setString(4, seatNumber);
            pstmt.executeUpdate();

            // Retrieve the auto-generated unique ID from Database
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Failed to save booking to database: " + e.getMessage());
        }
        return -1;
    }

    public static List<String> getBookedSeats(String movieTitle) {
        List<String> bookedSeats = new ArrayList<>();
        String query = "SELECT seat_number FROM bookings WHERE movie_title = ?";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, movieTitle);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bookedSeats.add(rs.getString("seat_number"));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch booked seats: " + e.getMessage());
        }
        return bookedSeats;
    }
}

class BookingService {
    public Booking createBooking(User user, Show show, String seatNumber) {
        Seat seat = show.getSeat(seatNumber);
        
        // Validation check to prevent double booking bug
        if (seat == null || seat.isBooked()) {
            return null;
        }

        // Save into DB and retrieve real unique booking ID
        int generatedId = DatabaseHandler.saveBooking(user.getName(), user.getEmail(), show.getMovie().getTitle(), seatNumber);
        
        if (generatedId == -1) {
            return null; // DB save error
        }

        seat.setBooked(true);
        String bookingId = "B" + generatedId;

        return new Booking(bookingId, user, show, seat);
    }
}

public class Movie_Ticket_Booking_System extends JFrame {
    private Show show1;
    private BookingService bookingService;
    private JComboBox<String> seatComboBox;
    private JTextField nameField;
    private JTextField emailField;

    public Movie_Ticket_Booking_System() {
        // Initialize DB schema
        DatabaseHandler.initializeDatabase();

        Theater t1 = new Theater("T1", "PVR", "Chennai");
        Movie m1 = new Movie("M1", "Leo", 150, "Tamil");
        show1 = new Show("S1", m1, t1);
        bookingService = new BookingService();

        // Load previously booked seats from DB on launch
        syncBookedSeatsWithDatabase();

        setTitle("Movie Ticket Booking System");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(41, 128, 185));
        JLabel headerLabel = new JLabel("Movie Ticket Booking System");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center Panel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(6, 2, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        centerPanel.add(new JLabel("Movie Title:"));
        centerPanel.add(new JLabel(show1.getMovie().getTitle() + " (" + show1.getMovie().getLanguage() + ")"));

        centerPanel.add(new JLabel("Duration:"));
        centerPanel.add(new JLabel(show1.getMovie().getDuration() + " mins"));

        centerPanel.add(new JLabel("Theater:"));
        centerPanel.add(new JLabel(show1.getTheater().getName() + ", " + show1.getTheater().getLocation()));

        centerPanel.add(new JLabel("Your Name:"));
        nameField = new JTextField();
        centerPanel.add(nameField);

        centerPanel.add(new JLabel("Your Email:"));
        emailField = new JTextField();
        centerPanel.add(emailField);

        centerPanel.add(new JLabel("Select Seat:"));
        seatComboBox = new JComboBox<>();
        updateSeatOptions();
        centerPanel.add(seatComboBox);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel();
        JButton bookButton = new JButton("Book Ticket");
        bookButton.setFont(new Font("Arial", Font.BOLD, 14));
        bookButton.setBackground(new Color(46, 204, 113));
        bookButton.setForeground(Color.WHITE);

        bookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleBooking();
            }
        });

        bottomPanel.add(bookButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void syncBookedSeatsWithDatabase() {
        List<String> bookedSeats = DatabaseHandler.getBookedSeats(show1.getMovie().getTitle());
        for (String seatNo : bookedSeats) {
            Seat s = show1.getSeat(seatNo);
            if (s != null) {
                s.setBooked(true);
            }
        }
    }

    private void updateSeatOptions() {
        seatComboBox.removeAllItems();
        for (Seat seat : show1.getSeats()) {
            if (!seat.isBooked()) {
                seatComboBox.addItem(seat.getSeatNumber());
            }
        }
    }

    private void handleBooking() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String selectedSeat = (String) seatComboBox.getSelectedItem();

        if (name.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your name and email.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedSeat == null) {
            JOptionPane.showMessageDialog(this, "No seats available for booking.", "Booking Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Customer customer = new Customer(2, name, email);
        Booking booking = bookingService.createBooking(customer, show1, selectedSeat);

        if (booking != null) {
            JOptionPane.showMessageDialog(this, booking.getBookingDetails() + "\n(Saved to Database)", "Booking Success", JOptionPane.INFORMATION_MESSAGE);
            updateSeatOptions();
            nameField.setText("");
            emailField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to book seat.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Movie_Ticket_Booking_System().setVisible(true);
        });
    }
}