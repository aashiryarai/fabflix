import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.*;

public class ActorsParser extends DefaultHandler {

    private Connection connection;
    private Map<String, String> existingStarNames = new HashMap<>();
    private int currentStarId = 0;
    private PrintWriter errorLog;
    private String tempVal;

    private String stageName;
    private String dobStr;

    private PreparedStatement insertWithDob;
    private PreparedStatement insertWithoutDob;

    private int insertedCount = 0;
    private int skippedCount = 0;
    private int batchSize = 100;
    private int actorCounter = 0;

    public ActorsParser(Connection conn) {
        this.connection = conn;
    }

    public void run() {
        try {
            errorLog = new PrintWriter(new FileWriter("invalid_actors.txt", true));
            loadExistingStarNames();
            loadMaxStarId();
            connection.setAutoCommit(false);
            insertWithDob = connection.prepareStatement(
                    "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)");
            insertWithoutDob = connection.prepareStatement(
                    "INSERT INTO stars (id, name) VALUES (?, ?)");

            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            sp.parse(new File("stanford-movies/actors63.xml"), this);
            insertWithDob.executeBatch();
            insertWithoutDob.executeBatch();
            connection.commit();

            System.out.printf("Actors import: Inserted: %d | Skipped: %d%n", insertedCount, skippedCount);
            //System.out.println("HERERRERE");
            errorLog.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void loadExistingStarNames() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM stars")) {
            while (rs.next()) {
                existingStarNames.put(rs.getString("name").trim(), rs.getString("id"));
                //System.out.println("HELLO")

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMaxStarId() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM stars WHERE id LIKE 'nm%'")) {
            if (rs.next()) {
                String maxId = rs.getString(1);
                if (maxId != null) {
                    currentStarId = Integer.parseInt(maxId.replaceAll("\\D+", ""));
                }
                //System.out.println("HELLO")
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String generateNextStarId() {
        currentStarId++;
        return String.format("nm%07d", currentStarId);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = "";
        if (qName.equalsIgnoreCase("actor")) {
            stageName = null;
            dobStr = null;
        }
    }

    public void characters(char[] ch, int start, int length) {
        tempVal += new String(ch, start, length);
    }
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("stagename")) {
            stageName = tempVal.trim();
        } else if (qName.equalsIgnoreCase("dob")) {
            dobStr = tempVal.trim();
            //System.out.println("dobStr");
        } else if (qName.equalsIgnoreCase("actor")) {
            processActor();
        }
    }

    private void processActor() {
        if (stageName == null || stageName.isEmpty()) {
            errorLog.println("Skipped actor: Missing name");
            skippedCount++;
            return;
        }
        if (existingStarNames.containsKey(stageName)) {
            errorLog.printf("Skipped dup actor: %s%n", stageName);
            skippedCount++;
            return;
        }
        String newId = generateNextStarId();

        try {
            if (dobStr != null && !dobStr.isEmpty()) {
                int birthYear = Integer.parseInt(dobStr);
                insertWithDob.setString(1, newId);
                insertWithDob.setString(2, stageName);
                insertWithDob.setInt(3, birthYear);
                insertWithDob.addBatch();
            } else {
                insertWithoutDob.setString(1, newId);
                insertWithoutDob.setString(2, stageName);
                insertWithoutDob.addBatch();
            }
        } catch (NumberFormatException e) {
            errorLog.printf("Invalid birthday for star '%s': '%s'. Inserted without dob.%n", stageName, dobStr);
            try {
                insertWithoutDob.setString(1, newId);
                insertWithoutDob.setString(2, stageName);
                insertWithoutDob.addBatch();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        existingStarNames.put(stageName, newId);
        insertedCount++;
        actorCounter++;
        if (actorCounter % batchSize == 0) {
            try {
                insertWithDob.executeBatch();
                insertWithoutDob.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
            new ActorsParser(conn).run();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
