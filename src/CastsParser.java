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

    public CastsParser(Connection conn) {
        this.connection = conn;
    }

    public void run() {
        loadExistingRelations();
        loadStarsIntoMap();
        loadValidMovieIds();
        parseXmlFile("stanford-movies/casts124.xml");
        parseDocument();
    }

    private void loadExistingRelations() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT starId, movieId FROM stars_in_movies")) {

            while (rs.next()) {
                String key = rs.getString("starId") + "|" + rs.getString("movieId");
                existingPairs.add(key);
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

        try {
            connection.setAutoCommit(false);
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)"
            );

            for (int i = 0; i < directorList.getLength(); i++) {
                Element dirfilms = (Element) directorList.item(i);
                NodeList castEntries = dirfilms.getElementsByTagName("m");

                for (int j = 0; j < castEntries.getLength(); j++) {
                    Element castEntry = (Element) castEntries.item(j);
                    String fid = getTextValue(castEntry, "f");
                    String actorName = getTextValue(castEntry, "a");

                    if (fid == null || actorName == null || actorName.trim().isEmpty()) {
                        System.out.printf("⚠️ Skipped cast with missing fid or actor: [%s / %s]%n", fid, actorName);
                        continue;
                    }

                    if (!validMovieIds.contains(fid)) {
                        System.out.printf("⚠️ Movie not found for fid '%s'. Skipping actor '%s'.%n", fid, actorName);
                        continue;
                    }

                    String trimmedName = actorName.trim();
                    String starId = starNameToId.get(trimmedName);
                    if (starId == null) {
                        System.out.printf("⚠️ Star not found for actor '%s' (fid: %s)%n", actorName, fid);
                        continue;
                    }

                    String pairKey = starId + "|" + fid;
                    if (!existingPairs.contains(pairKey)) {
                        insert.setString(1, starId);
                        insert.setString(2, fid);
                        insert.addBatch();
                        existingPairs.add(pairKey);
                    }
                }
            }

            insert.executeBatch();
            connection.commit();

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
            new CastsParser(conn).run();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
