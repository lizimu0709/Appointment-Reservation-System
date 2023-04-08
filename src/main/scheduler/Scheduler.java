package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
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
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        //Extra credit to add guidelines for strong passwords
        if (password.length() < 8) {
            System.out.println("A strong password has to be at least 8 characters.");
            System.out.println("Please try again!");
            return;
        }
        int lower = 0;
        int upper = 0;
        int number = 0;
        int special = 0;
        for (int i = 0; i < password.length(); i++) {
            //lowercase
            if (password.charAt(i) >= 97 && password.charAt(i) <= 122 ) {
                lower++;
            }
            //uppercase
            if (password.charAt(i) >= 65 && password.charAt(i) <= 90 ) {
                upper++;
            }
            //number
            if (password.charAt(i) >= 48 && password.charAt(i) <= 57 ) {
                number++;
            }
            //special characters
            if (password.charAt(i) == 33 || password.charAt(i) == 42
                    ||password.charAt(i) == 47 || password.charAt(i) == 95) {
                special++;
            }
        }
        if(lower == 0 ){
            System.out.println("A strong password should include lowercase letter.");
            System.out.println("Please try again!");
            return;
        }else if(upper == 0){
            System.out.println("A strong password should include uppercase letter.");
            System.out.println("Please try again!");
            return;
        }else if(number == 0){
            System.out.println("A strong password should include a number.");
            System.out.println("Please try again!");
            return;
        }else if(special == 0){
            System.out.println("A strong password has to included of at least one special character, from “!”, “@”, “#”, “?”.");
            System.out.println("Please try again!");
            return;
        }
        //check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the Patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println("Created Patient user " + username);
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
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
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
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

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
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
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged-in!");
            System.out.println("Please logout first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Patient Login failed.");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            System.out.println("Please logout first!");
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
            System.out.println("Caregiver Login failed.");
        } else {
            System.out.println("Caregiver Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // check 1: If no user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // check 2: Check the input format
        if (tokens.length != 2) {
            System.out.println("Invalid input format, please try again!");
            System.out.println("The right format is: search_caregiver_schedule year-month-day");
            return;
        }
        // check 3: Check if the date is future time and matches the real calendar
        try {
            LocalDate today = LocalDate.now();
            System.out.println("The current time is: " + today);
            System.out.println("The input time is:   " + LocalDate.parse(tokens[1]));
            LocalDate date = LocalDate.parse(tokens[1]);
            if (date.isBefore(today)) {
                System.out.println("Past results cannot be displayed!");
                return;
            }
        } catch (DateTimeParseException e) {
            System.out.println("Invalid input date, please try again!");
            System.out.println("The right format is: search_caregiver_schedule year-month-day");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            //PreparedStatement statement = con.prepareStatement(getSchedule);
            PreparedStatement statement = con.prepareStatement("SELECT DISTINCT A.Username, V.Name, V.Doses FROM Availabilities AS A, Vaccines AS V, Caregivers As C WHERE A.Time = ? ORDER BY A.Username");
            statement.setString(1, tokens[1]);
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-mm-dd");
            java.util.Date sqlDate = sdf1.parse(tokens[1]);
            ResultSet rs = statement.executeQuery();
            if(!rs.next()) {
                System.out.println("No result for such date!");
            } else {
                    do{
                    System.out.println("Care_username : " + rs.getString(1) +
                            " Vaccine name : " + rs.getString(2) +
                            " Number of doses : " + rs.getInt(3));
                } while (rs.next());
            }
            con.close();
        } catch (SQLException | ParseException e) {
            System.out.println("Error occurred when searching caregiver schedule!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // check 1: If no user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        //check 2: check login as patient
        if (currentCaregiver != null && currentPatient == null) {
            System.out.println("Please logout caregiver and login as a patient!");
            return;
        }
        //check 3: check input format
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        // check 4: Check if the date is future time and matches the real calendar
        try {
            LocalDate today = LocalDate.now();
            System.out.println("The current time is: " + today);
            System.out.println("The input time is:   " + LocalDate.parse(tokens[1]));
            LocalDate date = LocalDate.parse(tokens[1]);
            if (date.isBefore(today)) {
                System.out.println("Past results cannot be displayed!");
                return;
            }
        } catch (DateTimeParseException e) {
            System.out.println("Invalid input date, please try again!");
            System.out.println("The right format is: reserve year-month-day vaccine ");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];
        Random ran = new Random();
        int appoID = ran.nextInt(99999999);
        String patientName = currentPatient.getUsername();

        // Get caregiver name
        ArrayList<String> care_nameList = new ArrayList<>();
        ConnectionManager cm1 = new ConnectionManager();
        Connection con1 = cm1.createConnection();
        try {
            PreparedStatement statement = con1.prepareStatement("SELECT A.Time, A.Username FROM Availabilities AS A WHERE A.Time = ? Order By A.Username");
            statement.setString(1, date);
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-mm-dd");
            java.util.Date sqlDate = sdf1.parse(date);
            ResultSet rs = statement.executeQuery();
            if(!rs.next()) {
                System.out.println("There are no caregivers available for this date!");
            } else {
                do{
                    System.out.println("Time : " + rs.getString(1) +
                            " Care_username : " + rs.getString(2));
                    care_nameList.add(rs.getString(2));
                } while (rs.next());
            }
            con1.close();
        } catch (SQLException | ParseException e) {
            System.out.println("Error occurred when getting caregiver's name!");
            e.printStackTrace();
        } finally {
            cm1.closeConnection();
        }

        // Show doses from Vaccine
        ConnectionManager cm4 = new ConnectionManager();
        Connection con4 = cm4.createConnection();
        int dosesNum = 0;
        try {
            PreparedStatement statement = con4.prepareStatement("SELECT V.Doses FROM Vaccines AS V WHERE V.Name = ?");
            statement.setString(1, vaccineName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                dosesNum = rs.getInt(1);
            }
            con4.close();
        } catch (SQLException e) {
            System.out.println("Error occurred when reserving!");
            e.printStackTrace();
        } finally {
            cm4.closeConnection();
        }
        //check 4: Check the numbers of doses
        if (dosesNum == 0) {
            System.out.println("Not enough available doses!");
            return;
        }

        // Add value into appointment table
        if(!care_nameList.isEmpty()){
            String care_name = care_nameList.get(0);
            ConnectionManager cm2 = new ConnectionManager();
            Connection con2 = cm2.createConnection();
            try {
                PreparedStatement statement = con2.prepareStatement("INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)");
                statement.setInt(1, appoID);
                statement.setString(2, date);
                statement.setString(3, patientName);
                statement.setString(4, care_name);
                statement.setString(5, vaccineName);
                statement.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding value into appointment table!");
                e.printStackTrace();
            } finally {
                cm2.closeConnection();
            }

            // Delete value into Availabilities table
            ConnectionManager cm3 = new ConnectionManager();
            Connection con3 = cm3.createConnection();
            try {
                PreparedStatement statement = con3.prepareStatement("DELETE FROM Availabilities WHERE Time = ? AND Username = ?");
                statement.setString(1, date);
                statement.setString(2, care_name);
                statement.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error occurred when deleting value into Availabilities table!");
                e.printStackTrace();
            } finally {
                cm3.closeConnection();
            }

            // update value into vaccine table
            ConnectionManager cm5 = new ConnectionManager();
            Connection con5 = cm5.createConnection();
            try {
                PreparedStatement statement = con5.prepareStatement("UPDATE Vaccines SET Doses = ? WHERE name = ?");
                statement.setInt(1, dosesNum - 1);
                statement.setString(2, vaccineName);
                statement.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error occurred when updating value into vaccine table!");
                e.printStackTrace();
            } finally {
                cm5.closeConnection();
            }

            // output
            System.out.println("Appointment ID: " + appoID + ", Caregiver Username: " + care_name);
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
        // TODO: Extra credit
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

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // check 1: the length for tokens need to be exactly one
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            System.out.println("The correct input format is: show_appointments");
            return;
        }
        // check 2: the user should be login, output should be separately shown according to patient or caregiver
        if (currentPatient != null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            try {
                PreparedStatement statement = con.prepareStatement("SELECT appoID, Vaccine, Time, Caregiver FROM Appointments");
                ResultSet rs = statement.executeQuery();

                if(!rs.next()) {
                    System.out.println("There are no result for this patient!");
                } else {
                    do{
                        System.out.println("Appointment ID: " + rs.getInt(1) +
                                " Vaccine name: " + rs.getString(2)+
                                " Appointment date: " + rs.getString(3) +
                                " Caregiver name: " + rs.getString(4));
                    } while (rs.next());
                }
                con.close();
            } catch (SQLException e) {
                System.out.println("Error occurred when showing patient appointments");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }

        } else if (currentCaregiver != null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            try {
                PreparedStatement statement = con.prepareStatement("SELECT appoID, Vaccine, Time, Caregiver FROM Appointments");
                ResultSet rs = statement.executeQuery();

                if(!rs.next()) {
                    System.out.println("There are no result for this caregiver!");
                } else {
                    do{
                        System.out.println("Appointment ID: " + rs.getInt(1) +
                                " Vaccine name: " + rs.getString(2) +
                                " Appointment date: " + rs.getString(3) +
                                " Patient name: " + rs.getString(4));
                    } while (rs.next());
                }
                con.close();
            } catch (SQLException e) {
                System.out.println("Error occurred when showing caregiver appointments");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        } else {
            System.out.println("Please log in first!");
            return;
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // check 1: check if the current user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // check 2: check if the user print the right operation name
        else if((tokens.length == 1 && currentCaregiver != null)
        || (tokens.length == 1 && currentPatient != null)) {
            System.out.println("Ready to log out...");
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out!");
        }
        else{
            System.out.println("Error occurred when logged out, please try again!");
            return;
        }
    }
}
