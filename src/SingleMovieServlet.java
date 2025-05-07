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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String movieId = request.getParameter("id");
        request.getServletContext().log("Getting movie id: " + movieId);

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("SELECT ")
                    .append("m.id, m.title, m.year, m.director, r.rating, ")
                    // Genres: all genres, sorted alphabetically
                    .append("(SELECT GROUP_CONCAT(")
                    .append("    CONCAT('<a href=\"browse-genre.html?genre=', g.name, '\">', g.name, '</a>') ")
                    .append("    ORDER BY g.name SEPARATOR ', ') ")
                    .append(" FROM genres_in_movies gim ")
                    .append(" JOIN genres g ON gim.genreId = g.id ")
                    .append(" WHERE gim.movieId = m.id) AS genres, ")
                    // Stars: all stars, sorted by movie count desc, name asc
                    .append("(SELECT GROUP_CONCAT(html_name SEPARATOR ', ') FROM (")
                    .append("    SELECT CONCAT('<a href=\"single-star.html?id=', s.id, '\">', s.name, '</a>') AS html_name ")
                    .append("    FROM stars s ")
                    .append("    JOIN stars_in_movies sim1 ON s.id = sim1.starId ")
                    .append("    WHERE sim1.movieId = m.id ")
                    .append("    GROUP BY s.id ")
                    .append("    ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) DESC, s.name ASC ")
                    .append(") AS limited_stars) AS stars ")
                    .append("FROM movies m ")
                    .append("JOIN ratings r ON m.id = r.movieId ")
                    .append("WHERE m.id = ? ");

            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());
            statement.setString(1, movieId);

            ResultSet rs = statement.executeQuery();
            JsonArray jsonArray = new JsonArray();

            if (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", rs.getString("id"));
                jsonObject.addProperty("movie_title", rs.getString("title"));
                jsonObject.addProperty("movie_year", rs.getInt("year"));
                jsonObject.addProperty("movie_director", rs.getString("director"));
                jsonObject.addProperty("movie_rating", rs.getFloat("rating"));
                jsonObject.addProperty("movie_genres", rs.getString("genres"));
                jsonObject.addProperty("movie_stars", rs.getString("stars"));
                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();

            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            out.write(error.toString());
            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
