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

@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
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

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");

        boolean hasAtLeastOneCondition = (title != null && !title.isEmpty()) ||
                (year != null && !year.isEmpty()) ||
                (director != null && !director.isEmpty()) ||
                (star != null && !star.isEmpty());

        if (!hasAtLeastOneCondition) {
            response.setStatus(400);
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", "At least one search parameter (title, year, director, star) must be provided.");
            out.write(error.toString());
            out.close();
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating, ")
                    .append("GROUP_CONCAT(DISTINCT CONCAT('<a href=\"browse-genre.html?genre=', g.name, '\">', g.name, '</a>') ORDER BY g.name SEPARATOR ', ') AS genres, ")
                    .append("GROUP_CONCAT(DISTINCT CONCAT('<a href=\"single-star.html?id=', s.id, '\">', s.name, '</a>') ORDER BY s.name SEPARATOR ', ') AS stars ")
                    .append("FROM movies m ")
                    .append("JOIN ratings r ON m.id = r.movieId ")
                    .append("LEFT JOIN genres_in_movies gm ON m.id = gm.movieId ")
                    .append("LEFT JOIN genres g ON gm.genreId = g.id ")
                    .append("LEFT JOIN stars_in_movies sm ON m.id = sm.movieId ")
                    .append("LEFT JOIN stars s ON sm.starId = s.id ")
                    .append("WHERE 1=1 ");

            if (title != null && !title.isEmpty()) {
                queryBuilder.append("AND m.title LIKE ? ");
            }
            if (year != null && !year.isEmpty()) {
                queryBuilder.append("AND m.year = ? ");
            }
            if (director != null && !director.isEmpty()) {
                queryBuilder.append("AND m.director LIKE ? ");
            }
            if (star != null && !star.isEmpty()) {
                queryBuilder.append("AND s.name LIKE ? ");
            }

            queryBuilder.append("GROUP BY m.id, m.title, m.year, m.director, r.rating ")
                    .append("ORDER BY r.rating DESC LIMIT 20");

            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());
            int index = 1;

            if (title != null && !title.isEmpty()) {
                statement.setString(index++, "%"+title + "%");
            }
            if (year != null && !year.isEmpty()) {
                statement.setInt(index++, Integer.parseInt(year));
            }
            if (director != null && !director.isEmpty()) {
                statement.setString(index++, "%" + director +"%");
            }
            if (star != null && !star.isEmpty()) {
                statement.setString(index++, "%" +star + "%");
            }

            ResultSet rs = statement.executeQuery();
            JsonArray jsonArray = new JsonArray();
            while (rs.next()) {
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

            request.getServletContext().log("getting " + jsonArray.size() + " results");
            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
