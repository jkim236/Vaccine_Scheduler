package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.util.*;
import java.sql.*;

public class Patient {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    private Patient(PatientBuilder builder) {
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Patient(PatientGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addPatient = "INSERT INTO Patient (username, salt, hash) VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addPatient);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
    public int getPatientIdFromCredentials() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getPatientId = "SELECT patientId FROM Patient WHERE Username = ? AND Salt = ? AND Hash = ?";
        try {
            PreparedStatement statement = con.prepareStatement(getPatientId);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("PatientId");
            } else {
                throw new SQLException("No patient found with these credentials");
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            cm.closeConnection();
        }
    }


    public static class PatientBuilder {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        public PatientBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Patient build() {
            return new Patient(this);
        }
    }

    public static class PatientGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;
        private int patientId;

        public PatientGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Patient get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getPatient = "SELECT salt, hash FROM Patient WHERE username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getPatient);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] saltFromDB = resultSet.getBytes("salt");
                    byte[] hashFromDB = resultSet.getBytes("hash");
                    byte[] calculatedHash = Util.generateHash(password, saltFromDB);
                    if (Arrays.equals(hashFromDB, calculatedHash)) {
                        return new PatientBuilder(username, saltFromDB, hashFromDB).build();
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException(e.getMessage());
            } finally {
                cm.closeConnection();
            }
        }
    }
}


