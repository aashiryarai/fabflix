import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import org.xml.sax.InputSource;

public class ActorsParser {

    private Document dom;
    private Connection connection;
    private Map<String, String> existingStarNames = new HashMap<>();
    private int currentStarId = 0;
    private PrintWriter errorLog;

    public ActorsParser(Connection conn) {
        this.connection = conn;
    }

    public void run() {
        try {
            errorLog = new PrintWriter(new FileWriter("invalid_actors.txt", true));
        } catch (IOException e) {
            System.out.println("Could not open invalid_actors.txt for writing.");
            return;
        }

        loadExistingStarNames();
        loadMaxStarId();
        parseXmlFile("stanford-movies/actors63.xml");
        parseDocument();
        errorLog.close();
    }

    private void loadExistingStarNames() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM stars")) {
            while (rs.next()) {
                existingStarNames.put(rs.getString("name").trim(), rs.getString("id"));
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String generateNextStarId() {
        currentStarId++;
        return String.format("nm%07d", currentStarId);
    }

    private void parseXmlFile(String filePath) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new FileInputStream(filePath));
            is.setEncoding("ISO-8859-1");
            dom = db.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseDocument() {
        Element root = dom.getDocumentElement();
        NodeList actorList = root.getElementsByTagName("actor");

        int insertedCount = 0;
        int skippedCount = 0;
        int batchSize = 500;
        int actorCounter = 0;

        try {
            connection.setAutoCommit(false);
            PreparedStatement insertWithDob = connection.prepareStatement(
                    "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)");
            PreparedStatement insertWithoutDob = connection.prepareStatement(
                    "INSERT INTO stars (id, name) VALUES (?, ?)");

            for (int i = 0; i < actorList.getLength(); i++) {
                Element actor = (Element) actorList.item(i);
                String name = getTextValue(actor, "stagename");
                String dobStr = getTextValue(actor, "dob");

                if (name == null || name.trim().isEmpty()) {
                    errorLog.printf("Skipped actor at index %d: Missing name%n", i);
                    skippedCount++;
                    continue;
                }

                name = name.trim();
                if (existingStarNames.containsKey(name)) {
                    errorLog.printf("Skipped duplicate actor: %s%n", name);
                    skippedCount++;
                    continue;
                }

                String newId = generateNextStarId();

                try {
                    if (dobStr != null && !dobStr.trim().isEmpty()) {
                        int birthYear = Integer.parseInt(dobStr.trim());
                        insertWithDob.setString(1, newId);
                        insertWithDob.setString(2, name);
                        insertWithDob.setInt(3, birthYear);
                        insertWithDob.addBatch();
                    } else {
                        insertWithoutDob.setString(1, newId);
                        insertWithoutDob.setString(2, name);
                        insertWithoutDob.addBatch();
                    }
                    existingStarNames.put(name, newId);
                    insertedCount++;
                } catch (NumberFormatException e) {
                    errorLog.printf("Invalid DOB for star '%s': '%s'. Inserted without DOB.%n", name, dobStr);
                    insertWithoutDob.setString(1, newId);
                    insertWithoutDob.setString(2, name);
                    insertWithoutDob.addBatch();
                    existingStarNames.put(name, newId);
                    insertedCount++;
                }

                actorCounter++;
                if (actorCounter % batchSize == 0) {
                    insertWithDob.executeBatch();
                    insertWithoutDob.executeBatch();
                    connection.commit();
                    System.out.printf("✔ Committed batch at actor %d%n", actorCounter);
                }
            }


            insertWithDob.executeBatch();
            insertWithoutDob.executeBatch();
            connection.commit();
            System.out.printf("✅ Actors import complete. Inserted: %d | Skipped: %d%n", insertedCount, skippedCount);
        } catch (SQLException e) {
            System.out.println("❌ Batch insert failed. Rolling back.");
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        }
    }

    private String getTextValue(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() > 0) {
            Node node = list.item(0).getFirstChild();
            if (node != null) {
                String value = node.getNodeValue();
                if (value != null) {
                    return value.trim();
                }
            }
        }
        return null;
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
