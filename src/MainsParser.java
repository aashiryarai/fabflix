import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.*;

public class MainsParser extends DefaultHandler {

    private Connection connection;
    private PrintWriter errorLog;
    private Set<String> existingMovieIds = new HashSet<>();
    private Set<String> existingGenres = new HashSet<>();

    private String currentElement = "";
    private String currentDirector = null;
    private String currentMovieId = null;
    private String currentTitle = null;
    private String currentYearStr = null;
    private List<String> currentGenres = new ArrayList<>();

    private PreparedStatement insertMovie;
    private PreparedStatement insertGenre;
    private PreparedStatement insertGenreInMovie;
    private PreparedStatement insertRating;

    public MainsParser(Connection conn) {
        this.connection = conn;
    }

    public void run() {
        try {
            errorLog = new PrintWriter(new FileWriter("invalid_movies.txt", true));
            loadExistingIds();

            connection.setAutoCommit(false);
            insertMovie = connection.prepareStatement("INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)");
            insertGenre = connection.prepareStatement("INSERT INTO genres (name) VALUES (?)");
            insertGenreInMovie = connection.prepareStatement("INSERT INTO genres_in_movies (genreId, movieId) SELECT g.id, ? FROM genres g WHERE g.name = ?");
            insertRating = connection.prepareStatement("INSERT INTO ratings (movieId, rating, numVotes) VALUES (?, ?, ?)");

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new File("stanford-movies/mains243.xml"), this);

            insertMovie.executeBatch();
            insertGenre.executeBatch();
            insertGenreInMovie.executeBatch();
            insertRating.executeBatch();
            connection.commit();
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

    private void loadExistingIds() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id FROM movies");
            while (rs.next()) existingMovieIds.add(rs.getString("id"));

            rs = stmt.executeQuery("SELECT name FROM genres");
            while (rs.next()) existingGenres.add(normalizeGenre(rs.getString("name")));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // SAX Handler Methods

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        currentElement = qName;
        if (qName.equalsIgnoreCase("film")) {
            currentMovieId = null;
            currentTitle = null;
            currentYearStr = null;
            currentGenres = new ArrayList<>();
        }
    }

    public void characters(char[] ch, int start, int length) {
        String content = new String(ch, start, length).trim();
        if (content.isEmpty()) return;

        switch (currentElement) {
            case "dirname": currentDirector = content; break;
            case "fid": currentMovieId = content; break;
            case "t": currentTitle = content; break;
            case "year": currentYearStr = content; break;
            case "cat": currentGenres.add(content); break;
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("film")) {
            processMovieEntry();
        }
        currentElement = "";
    }

    private void processMovieEntry() {
        if (currentMovieId == null || currentTitle == null || currentYearStr == null || currentDirector == null) {
            errorLog.printf("Missing: fid=%s, title=%s, year=%s, director=%s%n", currentMovieId, currentTitle, currentYearStr, currentDirector);
            return;
        }

        if (existingMovieIds.contains(currentMovieId)) {
            errorLog.printf("Duplicate movie ID: %s%n", currentMovieId);
            return;
        }

        int year;
        try {
            year = Integer.parseInt(currentYearStr);
            if (year < 1800 || year > 2100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            errorLog.printf("Invalid year for movie '%s' (fid: %s): %s%n", currentTitle, currentMovieId, currentYearStr);
            return;
        }

        try {
            insertMovie.setString(1, currentMovieId);
            insertMovie.setString(2, currentTitle);
            insertMovie.setInt(3, year);
            insertMovie.setString(4, currentDirector);
            insertMovie.addBatch();
            existingMovieIds.add(currentMovieId);

            double randomRating = 5.0 + (Math.random() * 4.9);
            int numVotes = 100 + (int)(Math.random() * 900);
            insertRating.setString(1, currentMovieId);
            insertRating.setDouble(2, Math.round(randomRating * 10.0) / 10.0);
            insertRating.setInt(3, numVotes);
            insertRating.addBatch();

            for (String rawGenre : currentGenres) {
                if (!isValidGenre(rawGenre)) {
                    errorLog.printf("Invalid genre '%s' for movie ID %s%n", rawGenre, currentMovieId);
                    continue;
                }
                String genre = normalizeGenre(rawGenre);
                if (!existingGenres.contains(genre)) {
                    insertGenre.setString(1, genre);
                    insertGenre.addBatch();
                    existingGenres.add(genre);
                }
                insertGenreInMovie.setString(1, currentMovieId);
                insertGenreInMovie.setString(2, genre);
                insertGenreInMovie.addBatch();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String normalizeGenre(String genre) {
        if (genre == null) return null;
        genre = genre.trim();
        if (genre.isEmpty()) return null;
        return genre.substring(0, 1).toUpperCase() + genre.substring(1).toLowerCase();
    }

    private boolean isValidGenre(String genre) {
        if (genre == null || genre.trim().isEmpty()) return false;
        String g = genre.trim().toLowerCase();
        return g.length() >= 2 && g.length() <= 20 &&
                !g.matches(".*\\d.*") &&
                !g.contains("xxx") &&
                !g.contains("porn") &&
                !g.matches(".*[;*<>].*");
    }

    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/moviedb", "mytestuser", "My6$Password");
            new MainsParser(conn).run();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
