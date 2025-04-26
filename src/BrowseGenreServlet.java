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
        resp.setContentType("application/json");

        String query =
                "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                        // Genres (top 3, hyperlinked)
                        "  (SELECT GROUP_CONCAT(" +
                        "     CONCAT('<a href=\"browse-genre.html?genre=', g.name, '\">', g.name, '</a>')" +
                        "     ORDER BY g.name SEPARATOR ', ' ) " +
                        "   FROM genres_in_movies gim " +
                        "   JOIN genres g ON gim.genreId = g.id " +
                        "   WHERE gim.movieId = m.id " +
                        "   LIMIT 3) AS genres, " +

                        // Stars (top 3 by movie count, hyperlinked)
                        "  (SELECT GROUP_CONCAT(html_name SEPARATOR ', ') FROM (" +
                        "     SELECT CONCAT('<a href=\"single-star.html?id=', s.id, '\">', s.name, '</a>') AS html_name " +
                        "     FROM stars s " +
                        "     JOIN stars_in_movies sim1 ON s.id = sim1.starId " +
                        "     WHERE sim1.movieId = m.id " +
                        "     GROUP BY s.id " +
                        "     ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) DESC, s.name ASC " +
                        "     LIMIT 3 " +
                        "  ) AS limited_stars) AS stars " +

                        "FROM movies m " +
                        "JOIN ratings r ON m.id = r.movieId " +
                        "JOIN genres_in_movies gim_filter ON m.id = gim_filter.movieId " +
                        "JOIN genres g_filter ON gim_filter.genreId = g_filter.id " +
                        "WHERE g_filter.name = ? " +
                        "ORDER BY m.title";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, genre);
            ResultSet rs = ps.executeQuery();

            JsonArray result = new JsonArray();
            while (rs.next()) {
                JsonObject movie = new JsonObject();
                movie.addProperty("movie_id", rs.getString("id"));
                movie.addProperty("movie_title", rs.getString("title"));
                movie.addProperty("movie_year", rs.getInt("year"));
                movie.addProperty("movie_director", rs.getString("director"));
                movie.addProperty("movie_rating", rs.getFloat("rating"));
                movie.addProperty("movie_genres", rs.getString("genres"));
                movie.addProperty("movie_stars", rs.getString("stars"));
                result.add(movie);
            }

            resp.getWriter().write(result.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
}
