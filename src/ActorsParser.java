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

    public ActorsParser(Connection conn) {
        this.connection = conn;
    }

    public void run() {
        loadExistingStarNames();
        loadMaxStarId();
        parseXmlFile("stanford-movies/actors63.xml");
        parseDocument();
    }

    private void loadExistingStarNames() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM stars")) {
            while (rs.next()) {
                existingStarNames.put(rs.getString("name"), rs.getString("id"));
            }
            System.out.println("Loaded " + existingStarNames.size() + " existing stars.");
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

        try {
            connection.setAutoCommit(false);
            PreparedStatement insertWithDob = connection.prepareStatement(
                    "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)"
            );
            PreparedStatement insertWithoutDob = connection.prepareStatement(
                    "INSERT INTO stars (id, name) VALUES (?, ?)"
            );

            for (int i = 0; i < actorList.getLength(); i++) {
                Element actor = (Element) actorList.item(i);
                String name = getTextValue(actor, "stagename");
                String dobStr = getTextValue(actor, "dob");

                if (name == null || name.trim().isEmpty()) {
                    System.out.println("⚠️ Skipped actor with missing name.");
                    skippedCount++;
                    continue;
                }

                if (existingStarNames.containsKey(name)) {
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
                    System.out.printf("⚠️ Invalid DOB '%s' for star '%s'. Inserting as NULL.%n", dobStr, name);
                    insertWithoutDob.setString(1, newId);
                    insertWithoutDob.setString(2, name);
                    insertWithoutDob.addBatch();
                    existingStarNames.put(name, newId);
                    insertedCount++;
                }
            }

            insertWithDob.executeBatch();
            insertWithoutDob.executeBatch();
            connection.commit();
            System.out.printf("✅ Done! Inserted: %d | Skipped: %d%n", insertedCount, skippedCount);

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
        if (list.getLength() > 0 && list.item(0).getFirstChild() != null) {
            return list.item(0).getFirstChild().getNodeValue().trim();
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/moviedb", "root", "Akash13579!");
            new ActorsParser(conn).run();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
