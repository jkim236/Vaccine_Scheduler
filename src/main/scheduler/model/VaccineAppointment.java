package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

public class VaccineAppointment {
    private int appointmentId;
    private Date date;
    private int patientId;
    private int caregiverId;
    private int vaccineId;

    public VaccineAppointment(int appointmentId, int patientId, int caregiverId, int vaccineId, Date date) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.caregiverId = caregiverId;
        this.vaccineId = vaccineId;
        this.date = date;
    }
    public Date getDate() {
        return date;
    }
    public int getAppointmentId() {
        return appointmentId;
    }
    public int getCaregiverId() {
        return caregiverId;
    }
    public int getVaccineId() {
        return vaccineId;
    }
    public int getPatientId() {
        return patientId;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setInt(1, this.appointmentId);
            statement.setInt(2, this.patientId);
            statement.setInt(3, this.caregiverId);
            statement.setInt(4, this.vaccineId);
            statement.setDate(5, new java.sql.Date(this.date.getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
}
