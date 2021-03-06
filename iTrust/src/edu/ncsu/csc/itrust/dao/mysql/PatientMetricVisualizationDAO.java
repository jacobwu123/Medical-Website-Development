package edu.ncsu.csc.itrust.dao.mysql;

import edu.ncsu.csc.itrust.DBUtil;
import edu.ncsu.csc.itrust.beans.PatientBean;
import edu.ncsu.csc.itrust.beans.loaders.PatientLoader;
import edu.ncsu.csc.itrust.dao.DAOFactory;
import edu.ncsu.csc.itrust.exception.DBException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * A DAO for aggregating patient metrics by their various attributes.
 */
public class PatientMetricVisualizationDAO {
    private DAOFactory factory;
    private PatientLoader patientLoader;

    /**
     * @param daoFactory factory to get DAOs
     */
    public PatientMetricVisualizationDAO(DAOFactory daoFactory) {
        this.factory = daoFactory;
        this.patientLoader = new PatientLoader();
    }

    private Map<String, Integer> formatSQLResponse(ResultSet rs) throws SQLException {
        Map<String, Integer> results = new TreeMap<>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                try {
                    Integer a = Integer.parseInt(o1);
                    Integer b = Integer.parseInt(o2);
                    return a.compareTo(b);
                } catch (NumberFormatException e) {
                    return o1.compareTo(o2);
                }
            }
        });
        while (rs.next()) {
            Object x = rs.getObject("x");

            // Set default value for column if none specified
            if (x.toString().equals("")) {
                x = "None";
            }
            results.put(x.toString(), rs.getInt("y"));
        }
        return results;
    }

    /**
     * Gets all patients that have a certain attribute
     *
     * @param attr an attribute name
     * @param value the value of the attribute
     * @return a list of {@link PatientBean} objectse
     * @throws DBException
     */
    public List<PatientBean> getAllPatientWithAttr(String attr, String value) throws DBException {
        Connection conn = null;
        PreparedStatement ps = null;

        String query = "SELECT patients.* ";

        if (value.equals("None")) {
            value = "";
        }

        if (attr.equals("age")) {
            query += "FROM patients WHERE FLOOR(DATEDIFF(NOW(), DateOfBirth) / 365.25) = 67";
        } else if (attr.equals("number of visits")) {
            query += "FROM officevisits, patients WHERE patients.MID = officevisits.PatientID GROUP BY MID HAVING COUNT(*) = " + value;
        } else if (attr.equals("CauseOfDeath")) {
            query += "FROM patients WHERE CauseOfDeath IN (SELECT Code FROM icdcodes WHERE Description = '" + value + "')";
        } else {
            query += "FROM patients WHERE " + attr + " = '" + value + "'";
        }

        try {
            conn = factory.getConnection();
            ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            List<PatientBean> matchingPatients = new ArrayList<PatientBean>();
            while (rs.next()) {
                matchingPatients.add(patientLoader.loadSingle(rs));
            }
            ps.close();
            return matchingPatients;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            DBUtil.closeConnection(conn, ps);
        }
    }

    /**
     * Get all patient by their ages
     *
     * @return a map of ages and counts
     * @throws DBException
     * @throws SQLException
     */
    public Map<String, Integer> getAllPatientsAges() throws DBException, SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = factory.getConnection();
        ps = conn.prepareStatement("SELECT age AS x, COUNT(*) AS y FROM (SELECT MID, FLOOR(DATEDIFF(NOW(), DateOfBirth) / 365.25) AS age FROM patients) AS age_table WHERE age is NOT NULL GROUP BY age;");
        ResultSet rs = ps.executeQuery();
        Map<String, Integer> result = formatSQLResponse(rs);
        ps.close();
        DBUtil.closeConnection(conn, ps);
        return result;
    }

    /**
     * Get all patients by their visits
     *
     * @return a map of office visits and counts
     * @throws DBException
     * @throws SQLException
     */
    public Map<String, Integer> getAllPatientsVisits() throws DBException, SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = factory.getConnection();
        ps = conn.prepareStatement("SELECT num_office_visits AS x, COUNT(*) as y FROM (SELECT MID, COUNT(officevisits.PatientID) AS num_office_visits FROM officevisits, patients WHERE patients.MID = officevisits.PatientID GROUP BY MID) AS ii GROUP BY num_office_visits ORDER BY num_office_visits ASC");
        ResultSet rs = ps.executeQuery();
        Map<String, Integer> result = formatSQLResponse(rs);
        ps.close();
        DBUtil.closeConnection(conn, ps);
        return result;
    }

    /**
     * Get all patients by their deaths
     *
     * @return a map of patient deaths and counts
     * @throws DBException
     * @throws SQLException
     */
    public Map<String, Integer> getAllPatientsDeath() throws DBException, SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = factory.getConnection();
        ps = conn.prepareStatement("SELECT icdcodes.Description AS x, COUNT(*) AS y FROM patients, icdcodes WHERE CauseOfDeath <> '' AND icdcodes.Code = CauseOfDeath GROUP BY CauseOfDeath");
        ResultSet rs = ps.executeQuery();
        Map<String, Integer> result = formatSQLResponse(rs);
        ps.close();
        DBUtil.closeConnection(conn, ps);
        return result;
    }

    /**
     * Get all patients by their states
     *
     * @return a map of states and counts
     * @throws DBException
     * @throws SQLException
     */
    public Map<String, Integer> getAllPatientsStates() throws DBException, SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = factory.getConnection();
        ps = conn.prepareStatement("SELECT icState AS x, COUNT(*) AS y FROM patients GROUP BY icState");
        ResultSet rs = ps.executeQuery();
        Map<String, Integer> result = formatSQLResponse(rs);
        ps.close();
        DBUtil.closeConnection(conn, ps);
        return result;
    }

    /**
     * Get all patients by their blood type
     *
     * @return a map of blood type and counts
     * @throws DBException
     * @throws SQLException
     */
    public Map<String, Integer> getAllPatientsBlood() throws DBException, SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = factory.getConnection();
        ps = conn.prepareStatement("SELECT BloodType AS x, COUNT(*) AS y FROM patients WHERE BloodType <> '' GROUP BY BloodType;");
        ResultSet rs = ps.executeQuery();
        Map<String, Integer> result = formatSQLResponse(rs);
        ps.close();
        DBUtil.closeConnection(conn, ps);
        return result;
    }

    /**
     * Get all patients by their religions
     *
     * @return a map of religions and counts
     * @throws DBException
     * @throws SQLException
     */
    public Map<String, Integer> getAllPatientsReligion() throws DBException, SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = factory.getConnection();
        ps = conn.prepareStatement("SELECT Religion AS x, COUNT(*) AS y FROM patients GROUP BY Religion");
        ResultSet rs = ps.executeQuery();
        Map<String, Integer> result = formatSQLResponse(rs);
        ps.close();
        DBUtil.closeConnection(conn, ps);
        return result;
    }

    /**
     * Get all patients by their languages
     *
     * @return a map of languages and counts
     * @throws DBException
     * @throws SQLException
     */
    public Map<String, Integer> getAllPatientsLanguage() throws DBException, SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = factory.getConnection();
        ps = conn.prepareStatement("SELECT Language AS x, COUNT(*) AS y FROM patients GROUP BY Language;");
        ResultSet rs = ps.executeQuery();
        Map<String, Integer> result = formatSQLResponse(rs);
        ps.close();
        DBUtil.closeConnection(conn, ps);
        return result;
    }
}
