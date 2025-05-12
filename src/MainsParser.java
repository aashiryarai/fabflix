import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import org.xml.sax.InputSource;

public class MainsParser {

    private Document dom;
    private Connection connection;
    private Set<String> existingMovieIds = new HashSet<>();
    private Set<String> existingGenres = new HashSet<>();
    private PrintWriter errorLog;

    public MainsParser(Connection conn) {
        this.connection = conn;
    }
    public void run() {
        try {
            errorLog = new PrintWriter(new FileWriter("invalid_movies.txt", true));
        } catch (IOException e) {
            System.out.println("Failed to open log file.");
            return;
        }

        loadExistingIds();
        parseXmlFile("stanford-movies/mains243.xml");
        parseDocument();
        errorLog.close();
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
        NodeList directorFilmsList = root.getElementsByTagName("directorfilms");

        try {
            connection.setAutoCommit(false);
            PreparedStatement insertMovie = connection.prepareStatement(
                    "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)");
            PreparedStatement insertGenre = connection.prepareStatement(
                    "INSERT INTO genres (name) VALUES (?)");
            PreparedStatement insertGenreInMovie = connection.prepareStatement(
                    "INSERT INTO genres_in_movies (genreId, movieId) SELECT g.id, ? FROM genres g WHERE g.name = ?");
            PreparedStatement insertRating = connection.prepareStatement(
                    "INSERT INTO ratings (movieId, rating, numVotes) VALUES (?, ?, ?)");

            for (int i = 0; i < directorFilmsList.getLength(); i++) {
                Element directorFilms = (Element) directorFilmsList.item(i);
                String directorName = getTextValue(directorFilms, "dirname");

                NodeList filmList = directorFilms.getElementsByTagName("film");
                for (int j = 0; j < filmList.getLength(); j++) {
                    Element film = (Element) filmList.item(j);
                    String fid = getTextValue(film, "fid");
                    String title = getTextValue(film, "t");
                    String yearStr = getTextValue(film, "year");
                    if (fid == null || fid.trim().isEmpty() || title == null || title.trim().isEmpty() || yearStr == null || yearStr.trim().isEmpty() || directorName == null || directorName.trim().isEmpty()) {
                        errorLog.printf("Missing: fid=%s, title=%s, year=%s, director=%s%n", fid, title, yearStr, directorName);
                        continue;
                    }

                    if (existingMovieIds.contains(fid)) {
                        errorLog.printf("Duplicate movie ID: %s%n", fid);
                        continue;
                    }

                    int year;
                    try {
                        year = Integer.parseInt(yearStr);
                        if (year < 1800 || year > 2100) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        errorLog.printf("Invalid year for movie '%s' (fid: %s): %s%n", title, fid, yearStr);
                        continue;
                    }

                    insertMovie.setString(1, fid);
                    insertMovie.setString(2, title);
                    insertMovie.setInt(3, year);
                    insertMovie.setString(4, directorName);
                    insertMovie.addBatch();
                    existingMovieIds.add(fid);

                    double randomRating = 5.0 + (Math.random() * 4.9);
                    int numVotes = 100 + (int)(Math.random() * 900);
                    insertRating.setString(1, fid);
                    insertRating.setDouble(2, Math.round(randomRating * 10.0) / 10.0);
                    insertRating.setInt(3, numVotes);
                    insertRating.addBatch();

                    Element cats = (Element) film.getElementsByTagName("cats").item(0);
                    if (cats != null) {
                        NodeList catList = cats.getElementsByTagName("cat");

                        for (int k = 0; k < catList.getLength(); k++) {
                            String rawGenre = catList.item(k).getTextContent();
                            if (!isValidGenre(rawGenre)) {
                                errorLog.printf("Invald genre '%s' for movie ID %s%n", rawGenre, fid);
                                continue;
                            }
                            String genre = normalizeGenre(rawGenre);

                            if (!existingGenres.contains(genre)) {
                                insertGenre.setString(1, genre);
                                insertGenre.addBatch();
                                existingGenres.add(genre);
                            }
                            insertGenreInMovie.setString(1, fid);
                            insertGenreInMovie.setString(2, genre);
                            insertGenreInMovie.addBatch();
                        }
                    }
                }
            }
            insertMovie.executeBatch();
            insertGenre.executeBatch();
            insertGenreInMovie.executeBatch();
            insertRating.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
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
