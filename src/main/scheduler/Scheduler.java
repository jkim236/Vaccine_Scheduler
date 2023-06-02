package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;


public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;
    private static Patient loggedInPatient;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        try {
            byte[] salt = Util.generateSalt();
            byte[] hash = Util.generateHash(password, salt);
            Patient newPatient = new Patient.PatientBuilder(username, salt, hash).build();
            newPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key value")) {
                System.out.println("Username taken, try again!");
            } else {
                System.out.println("Failed to create user.");
            }
        } catch (Exception e) {
            System.out.println("Failed to create user.");
        }
    }


    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }
    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patient WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }
    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregiver WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to login user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        try {
            if (currentPatient != null) {
                System.out.println("User already logged in.");
                return;
            }
            Patient patient = new Patient.PatientGetter(username, password).get();
            if (patient != null) {
                currentPatient = patient;
                System.out.println("Logged in as: " + username);
            } else {
                System.out.println("Login failed.");
            }
        } catch (Exception e) {
            System.out.println("Login failed.");
        }
    }


    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Please provide a date!");
            return;
        }

        Date date;
        try {
            date = Date.valueOf(tokens[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Please provide a date in the format: yyyy-mm-dd");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchSchedule = "SELECT c.username, a.dateAvailable " +
                "FROM Caregiver c JOIN Availabilities a ON c.username = a.username " +
                "WHERE a.dateAvailable = ? AND a.isAvailable = 1 " +
                "ORDER BY c.username";

        try {
            PreparedStatement statement = con.prepareStatement(searchSchedule);
            statement.setDate(1, date);
            ResultSet resultSet = statement.executeQuery();

            int count = 0;
            while (resultSet.next()) {
                String caregiverUsername = resultSet.getString("username");
                Date appointmentDate = resultSet.getDate("dateAvailable");

                System.out.println("Caregiver username: " + caregiverUsername);
                System.out.println("Appointment Date: " + appointmentDate);
                count ++;
            }
            if (count == 0) {
                System.out.println("No caregivers available on this date. Please try a different date!");
                return;
            }

        } catch (SQLException e) {
            System.out.println("Error while searching for schedules.");
            e.printStackTrace();
        }

        String searchVaccine = "SELECT v.name, v.quantity " +
                "FROM Vaccine v";

        try {
            PreparedStatement statement = con.prepareStatement(searchVaccine);
            ResultSet resultSet = statement.executeQuery();

            // Print results
            while (resultSet.next()) {
                String vaccineName = resultSet.getString("name");
                int availableDoses = resultSet.getInt("quantity");

                System.out.println("Vaccine name: " + vaccineName);
                System.out.println("Available doses: " + availableDoses);
            }

        } catch (SQLException e) {
            System.out.println("Error while searching for vaccines.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null && currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        }
        if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Please provide a date and vaccine name!");
            return;
        }

        // Convert input string to date
        Date date;
        try {
            date = Date.valueOf(tokens[1]);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Please provide a date in the format: yyyy-mm-dd");
            return;
        }

        // Get vaccine name
        String vaccineName = tokens[2];

        // Initialize connection to the database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            // Check if enough doses are available
            String checkDoses = "SELECT quantity, vaccineId FROM Vaccine WHERE name = ? AND quantity > 0";
            PreparedStatement checkDosesStatement = con.prepareStatement(checkDoses);
            checkDosesStatement.setString(1, vaccineName);
            ResultSet resultSet = checkDosesStatement.executeQuery();

            if (!resultSet.next()) {
                System.out.println("Not enough available doses!");
                return;
            }

            int vaccineId = resultSet.getInt("vaccineId");

            // Search for available caregivers on this date
            String searchSchedule = "SELECT TOP 1 c.username, c.caregiverId, a.isAvailable " +
                    "FROM Caregiver c JOIN Availabilities a ON c.username = a.username " +
                    "WHERE a.dateAvailable = ? AND a.isAvailable = 1 " +
                    "ORDER BY c.username";


            PreparedStatement statement = con.prepareStatement(searchSchedule);
            statement.setDate(1, date);
            resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                System.out.println("No Caregiver is available!");
                return;
            }

            String caregiverUsername = resultSet.getString("username");
            int caregiverId = resultSet.getInt("caregiverId");

            // Insert new appointment.
            String createAppointment = "INSERT INTO VaccineAppointment(patientId, date, caregiverId, vaccineId) VALUES (?, ?, ?, ?)";
            PreparedStatement createStatement = con.prepareStatement(createAppointment, Statement.RETURN_GENERATED_KEYS);
            createStatement.setInt(1, currentPatient.getPatientIdFromCredentials());
            createStatement.setDate(2, date);
            createStatement.setInt(3, caregiverId);
            createStatement.setInt(4, vaccineId);
            createStatement.executeUpdate();

            // Fetch the generated appointmentId.
            ResultSet generatedKeys = createStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int appointmentId = generatedKeys.getInt(1);
                System.out.println("Appointment ID: " + appointmentId + ", Caregiver username: " + caregiverUsername);

                // Update the Availabilities table to set isAvailable to 0 for this caregiver and date.
                String updateAvailability = "UPDATE Availabilities SET isAvailable = 0 WHERE username = ? AND dateAvailable = ?";
                PreparedStatement updateStatement = con.prepareStatement(updateAvailability);
                updateStatement.setString(1, caregiverUsername);
                updateStatement.setDate(2, date);
                updateStatement.executeUpdate();
            } else {
                throw new SQLException("Creating appointment failed, no ID obtained.");
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }



    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Please provide an appointment id!");
            return;
        }

        // Get appointment id from input
        int appointmentId;
        try {
            appointmentId = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid appointment id format. Please provide a valid appointment id.");
            return;
        }

        // Initialize connection to the database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            // Check if the appointment exists and belongs to the logged in user
            String checkAppointment = "SELECT va.*, c.username FROM VaccineAppointment va JOIN Caregiver c ON va.caregiverId = c.caregiverId WHERE va.appointmentId = ? AND (va.patientId = ? OR va.caregiverId = ?)";
            PreparedStatement checkStatement = con.prepareStatement(checkAppointment);
            checkStatement.setInt(1, appointmentId);

            if (currentPatient != null) {
                checkStatement.setInt(2, currentPatient.getPatientIdFromCredentials());
                checkStatement.setInt(3, currentPatient.getPatientIdFromCredentials());
            } else {
                checkStatement.setInt(2, currentCaregiver.getCaregiverIdFromCredentials());
                checkStatement.setInt(3, currentCaregiver.getCaregiverIdFromCredentials());
            }

            ResultSet resultSet = checkStatement.executeQuery();
            if (!resultSet.next()) {
                System.out.println("No such appointment found!");
                return;
            }

            Date appointmentDate = resultSet.getDate("date");
            int caregiverId = resultSet.getInt("caregiverId");
            String caregiverUsername = resultSet.getString("username"); // retrieve the caregiver's username

            // Delete the appointment
            String deleteAppointment = "DELETE FROM VaccineAppointment WHERE appointmentId = ?";
            PreparedStatement deleteStatement = con.prepareStatement(deleteAppointment);
            deleteStatement.setInt(1, appointmentId);
            deleteStatement.executeUpdate();

            // Update the caregiver's availability
            String updateAvailability = "UPDATE Availabilities SET isAvailable = 1 WHERE username = ? AND dateAvailable = ?";
            PreparedStatement updateStatement = con.prepareStatement(updateAvailability);
            updateStatement.setString(1, caregiverUsername);
            updateStatement.setDate(2, appointmentDate);
            updateStatement.executeUpdate();

            System.out.println("Appointment cancelled successfully.");

        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }



    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String role;
        String username;

        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        if (currentPatient != null) {
            role = "patient";
            username = "" + currentPatient.getPatientIdFromCredentials();
        }
        else {
            role = "caregiver";
            username = "" + currentCaregiver.getCaregiverIdFromCredentials();
        }

        try {
            PreparedStatement ps;

            if (role.equals("patient")) {
                String patientQuery = "SELECT va.appointmentId, v.name AS vaccine_name, va.date, c.username AS caregiver_name " +
                        "FROM VaccineAppointment va " +
                        "JOIN Vaccine v ON va.vaccineId = v.vaccineId " +
                        "JOIN Caregiver c ON va.caregiverId = c.caregiverId " +
                        "WHERE va.patientId = ? " +
                        "ORDER BY va.appointmentId";
                ps = con.prepareStatement(patientQuery);
            } else if (role.equals("caregiver")) {
                String caregiverQuery = "SELECT va.appointmentId, v.name AS vaccine_name, va.date, p.username AS patient_name " +
                        "FROM VaccineAppointment va " +
                        "JOIN Vaccine v ON va.vaccineId = v.vaccineId " +
                        "JOIN Patient p ON va.patientId = p.patientId " +
                        "WHERE va.caregiverId = ? " +
                        "ORDER BY va.appointmentId";
                ps = con.prepareStatement(caregiverQuery);
            } else {
                System.out.println("Please try again!");
                return;
            }
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                count ++;
                int appointmentID = rs.getInt("appointmentId");
                String vaccineName = rs.getString("vaccine_name");
                Date date = rs.getDate("date");
                String name = rs.getString("caregiver_name");
                if (role.equals("caregiver")) {
                    name = rs.getString("patient_name");
                    System.out.println("AppointmentID: " + appointmentID + " " + "vaccine name: " + vaccineName + " " + "date: " + date + " " + "patient name: " + name);
                }
                else {
                    name = rs.getString("caregiver_name");
                    System.out.println("AppointmentID: " + appointmentID + " " + "vaccine name: " + vaccineName + " " + "date: " + date + " " + "caregiver name: " + name);
                }
            }
            if (count == 0) {
                System.out.println("No appointments found");
                return;
            }

        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }


    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
        } else {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out!");
        }
    }
}
