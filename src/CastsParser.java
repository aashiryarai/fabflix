import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.*;

public class CastsParser extends DefaultHandler {
    private Connection connection;
    private Set<String> existingPairs = new HashSet<>();
    private Map<String, String> starNameToId = new HashMap<>();
    private Set<String> validMovieIds = new HashSet<>();
    private PrintWriter errorLog;
    private String currentElement = "";
    private String currentMovieId = null;
    private String currentActorName = null;
    private PreparedStatement insertStatement;
    private int castCounter =0;
    private final int batchSize = 100;

    public CastsParser(Connection connection) {
        this.connection = connection;
    }

    public void run() {
        try {
            errorLog = new PrintWriter(new FileWriter("invalid_casts.txt", true));
            loadExistingRelations();
            loadStarsIntoMap();
            loadValidMovieIds();

            connection.setAutoCommit(false);
            insertStatement = connection.prepareStatement(
                    "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)");

            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            sp.parse(new File("stanford-movies/casts124.xml"), this);

            insertStatement.executeBatch();
            connection.commit();
            errorLog.close();

            System.out.printf("Finished inserting %d valid star-movie pairs.%n", castCounter);
        } catch (Exception e) {
            e.printStackTrace();
            //System.err.println(e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        }
    }
    private void loadExistingRelations() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT starId, movieId FROM stars_in_movies")) {
            while (rs.next()) {
                existingPairs.add(rs.getString("starId") + "|" + rs.getString("movieId"));
                //System.out.println("starId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStarsIntoMap() {
        try(Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM stars")) {
            while(rs.next()) {
                starNameToId.put(rs.getString("name").trim(), rs.getString("id"));
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void loadValidMovieIds() {
        try(Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM movies")) {
            while (rs.next()) {
                validMovieIds.add(rs.getString("id"));
                //System.out.println(rs.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        currentElement = qName;
        if (qName.equalsIgnoreCase("m")) {
            currentMovieId = null;
            currentActorName = null;
        }
    }
    @Override
    public void characters (char[] ch, int start, int length) {
        String content = new String(ch, start, length).trim();
        if (content.isEmpty()) return;
        switch (currentElement) {
            case "f":
                currentMovieId = content;
                break;
            case "a":
                currentActorName = content;
                break;
        }
    }
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("m")) {
            processCastEntry();
        }
        currentElement = "";
    }

    //


    private void processCastEntry() {
        if (currentMovieId == null || currentActorName == null || currentActorName.trim().isEmpty()) {
            errorLog.printf("Missing fields: [fid: %s, actor: %s]%n", currentMovieId, currentActorName);
            return;
        }
        if (!validMovieIds.contains(currentMovieId)) {
            errorLog.printf("Invalid movieId: %s for actor: %s%n", currentMovieId, currentActorName);
            return;
        }
        String trimmedName = currentActorName.trim();
        String starId = starNameToId.get(trimmedName);
        if (starId == null) {
            errorLog.printf("Star name not found: %s (fid: %s)%n", trimmedName, currentMovieId);
            return;
        }
        String pairKey = starId+"|" +currentMovieId;
        if (existingPairs.contains(pairKey)) {
            errorLog.printf("Duplicate skip: (%s, %s)%n", starId, currentMovieId);
            return;
        }
        try {
            insertStatement.setString(1, starId);
            insertStatement.setString(2, currentMovieId);
            insertStatement.addBatch();
            existingPairs.add(pairKey);
            castCounter++;
            if (castCounter % batchSize == 0) {
                insertStatement.executeBatch();
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
