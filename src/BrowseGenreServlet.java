import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "BrowseGenreServlet", urlPatterns = "/api/browse-genre")
public class BrowseGenreServlet extends HttpServlet {
    private DataSource ds;

    public void init(ServletConfig config) {
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String genre = req.getParameter("genre");
        String sortBy = req.getParameter("sortBy");
        String sortOrder = req.getParameter("sortOrder");
        int page = Integer.parseInt(req.getParameter("page"));
        int pageSize = Integer.parseInt(req.getParameter("pageSize"));

        resp.setContentType("application/json");

        // Defaults
        if (sortBy == null || (!sortBy.equals("title") && !sortBy.equals("rating"))) {
            sortBy = "title";
        }
        if (sortOrder == null || (!sortOrder.equals("asc") && !sortOrder.equals("desc"))) {
            sortOrder = "asc";
        }

        int offset = (page - 1) * pageSize;

        String query =
                "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                        // First 3 genres alphabetically
                        " (SELECT GROUP_CONCAT(name SEPARATOR ', ') FROM ( " +
                        "     SELECT g.name " +
                        "     FROM genres_in_movies gim2 " +
                        "     JOIN genres g ON gim2.genreId = g.id " +
                        "     WHERE gim2.movieId = m.id " +
                        "     ORDER BY g.name ASC " +
                        "     LIMIT 3 " +
                        " ) AS genre_names) AS genres, " +
                        // First 3 stars by movie count desc, then name asc
                        " (SELECT GROUP_CONCAT(html_name SEPARATOR ', ') FROM ( " +
                        "     SELECT CONCAT('<a href=\"single-star.html?id=', s.id, '\">', s.name, '</a>') AS html_name " +
                        "     FROM stars s " +
                        "     JOIN stars_in_movies sim1 ON s.id = sim1.starId " +
                        "     WHERE sim1.movieId = m.id " +
                        "     GROUP BY s.id " +
                        "     ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) DESC, s.name ASC " +
                        "     LIMIT 3 " +
                        " ) AS star_names) AS stars " +
                        "FROM movies m " +
                        "JOIN ratings r ON m.id = r.movieId " +
                        "WHERE m.id IN ( " +
                        "    SELECT gim.movieId " +
                        "    FROM genres_in_movies gim " +
                        "    JOIN genres g ON gim.genreId = g.id " +
                        "    WHERE g.name = ? " +
                        ") " +
                        "ORDER BY " + (sortBy.equals("rating") ? "r.rating" : "m.title") + " " + sortOrder + ", " +
                        (sortBy.equals("rating") ? "m.title" : "r.rating") + " " + sortOrder + " " +
                        "LIMIT ? OFFSET ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, genre);
            ps.setInt(2, pageSize);
            ps.setInt(3, offset);

            ResultSet rs = ps.executeQuery();
            JsonArray result = new JsonArray();

            while (rs.next()) {
                JsonObject movie = new JsonObject();
                movie.addProperty("movie_id", rs.getString("id"));
                movie.addProperty("movie_title", rs.getString("title"));
                movie.addProperty("movie_year", rs.getInt("year"));
                movie.addProperty("movie_director", rs.getString("director"));
                movie.addProperty("movie_rating", rs.getFloat("rating"));
                movie.addProperty("movie_genres", rs.getString("genres")); // genre names only
                movie.addProperty("movie_stars", rs.getString("stars"));   // already hyperlinked
                result.add(movie);
            }

            resp.getWriter().write(result.toString());
        } catch (Exception e) {
            resp.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
}
