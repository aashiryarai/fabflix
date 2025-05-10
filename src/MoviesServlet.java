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

        String sortBy = request.getParameter("sortBy") == null ? "title" : request.getParameter("sortBy");
        String sortOrder = request.getParameter("sortOrder") == null ? "asc" : request.getParameter("sortOrder");
        int page = request.getParameter("page") == null ? 1 : Integer.parseInt(request.getParameter("page"));
        int pageSize = request.getParameter("pageSize") == null ? 10 : Integer.parseInt(request.getParameter("pageSize"));

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("SELECT m.id, m.title, m.year, m.director, r.rating, ")
                    // Genres: first 3, alphabetically
                    .append("(SELECT GROUP_CONCAT(")
                    .append("    CONCAT('<a href=\"browse-genre.html?genre=', g.name, '\">', g.name, '</a>') ")
                    .append("    ORDER BY g.name SEPARATOR ', ') ")
                    .append(" FROM genres_in_movies gim ")
                    .append(" JOIN genres g ON gim.genreId = g.id ")
                    .append(" WHERE gim.movieId = m.id ")
                    .append(" LIMIT 3) AS genres, ")
                    // Stars: first 3 by movie count
                    .append("(SELECT GROUP_CONCAT(html_name SEPARATOR ', ') FROM (")
                    .append("    SELECT CONCAT('<a href=\"single-star.html?id=', s.id, '\">', s.name, '</a>') AS html_name ")
                    .append("    FROM stars s ")
                    .append("    JOIN stars_in_movies sim1 ON s.id = sim1.starId ")
                    .append("    WHERE sim1.movieId = m.id ")
                    .append("    GROUP BY s.id ")
                    .append("    ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) DESC, s.name ASC ")
                    .append("    LIMIT 3 ")
                    .append(") AS limited_stars) AS stars ")
                    .append("FROM movies m ")
                    .append("LEFT JOIN ratings r ON m.id = r.movieId ");

            // Only join with stars if star search
            if (star != null && !star.isEmpty()) {
                queryBuilder.append("JOIN stars_in_movies sim ON m.id = sim.movieId ")
                        .append("JOIN stars s ON sim.starId = s.id ");
            }

            queryBuilder.append("WHERE 1=1 ");

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

            queryBuilder.append("GROUP BY m.id, m.title, m.year, m.director, r.rating ");

            // Sort
            if (sortBy.equals("rating")) {
                queryBuilder.append("ORDER BY r.rating ").append(sortOrder.toUpperCase()).append(", m.title ASC ");
            } else {
                queryBuilder.append("ORDER BY m.title ").append(sortOrder.toUpperCase()).append(", r.rating DESC ");
            }

            // Pagination
            queryBuilder.append("LIMIT ? OFFSET ?");

            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());
            int index = 1;

            if (title != null && !title.isEmpty()) {
                statement.setString(index++, "%" + title + "%");
            }
            if (year != null && !year.isEmpty()) {
                statement.setInt(index++, Integer.parseInt(year));
            }
            if (director != null && !director.isEmpty()) {
                statement.setString(index++, "%" + director + "%");
            }
            if (star != null && !star.isEmpty()) {
                statement.setString(index++, "%" + star + "%");
            }

            statement.setInt(index++, pageSize);
            statement.setInt(index++, (page - 1) * pageSize);

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
