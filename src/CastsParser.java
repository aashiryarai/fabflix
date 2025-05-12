import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import org.xml.sax.InputSource;

public class CastsParser {

    private Document dom;
    private Connection connection;
    private Set<String> existingPairs = new HashSet<>();
    private Map<String, String> starNameToId = new HashMap<>();
    private Set<String> validMovieIds = new HashSet<>();
    private PrintWriter errorLog;

    public CastsParser(Connection conn) {
        this.connection = conn;
    }

    public void run() {
        try {
            errorLog = new PrintWriter(new FileWriter("invalid_casts.txt", true));
        } catch (IOException e) {
            System.err.println("Failed to open invalid_casts.txt");
            return;
        }
        loadExistingRelations();
        loadStarsIntoMap();
        loadValidMovieIds();
        parseXmlFile("stanford-movies/casts124.xml");
        parseDocument();
        errorLog.close();
    }

    private void loadExistingRelations() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT starId, movieId FROM stars_in_movies")) {
            while (rs.next()) {
                existingPairs.add(rs.getString("starId") + "|" + rs.getString("movieId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void loadStarsIntoMap() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM stars")) {
            while (rs.next()) {
                starNameToId.put(rs.getString("name").trim(), rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void loadValidMovieIds() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM movies")) {
            while (rs.next()) {
                validMovieIds.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        NodeList directorList = root.getElementsByTagName("dirfilms");
        int batchSize = 500;
        int castCounter = 0;

        try {
            connection.setAutoCommit(false);
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)");

            for (int i = 0; i < directorList.getLength(); i++) {
                Element dirfilms = (Element) directorList.item(i);
                NodeList castEntries = dirfilms.getElementsByTagName("m");

                for (int j = 0; j < castEntries.getLength(); j++) {
                    Element castEntry = (Element) castEntries.item(j);
                    String fid = getTextValue(castEntry, "f");
                    String actorName = getTextValue(castEntry, "a");

                    if (fid == null || actorName == null || actorName.trim().isEmpty()) {
                        errorLog.printf("Missing fields: [fid: %s, actor: %s]%n", fid, actorName);
                        continue;
                    }
                    if (!validMovieIds.contains(fid)) {
                        errorLog.printf("Invalid movie ID: %s for actor: %s%n", fid, actorName);
                        continue;
                    }
                    String trimmedName = actorName.trim();
                    String starId = starNameToId.get(trimmedName);
                    if (starId == null) {
                        errorLog.printf("Star not found for name: %s (fid: %s)%n", trimmedName, fid);
                        continue;
                    }
                    String pairKey = starId + "|" + fid;
                    if (existingPairs.contains(pairKey)) {
                        errorLog.printf("Duplicate pair skipped: (%s, %s)%n", starId, fid);
                        continue;
                    }
                    insert.setString(1, starId);
                    insert.setString(2, fid);
                    insert.addBatch();
                    existingPairs.add(pairKey);
                    castCounter++;
                    if (castCounter % batchSize == 0) {
                        insert.executeBatch();
                        connection.commit();
                        //System.out.printf("Committed batch at cast %d%n", castCounter);
                    }
                }
            }

            insert.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            System.err.println("Batch insert failed");
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
            String value = list.item(0).getFirstChild().getNodeValue();
            return value != null ? value.trim() : null;
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
            new CastsParser(conn).run();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
