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

@WebServlet(name = "BrowseTitleServlet", urlPatterns = "/api/browse-title")
public class TitlesServlet extends HttpServlet {
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
        String startsWith = req.getParameter("startsWith");
        String sortBy = req.getParameter("sortBy") == null ? "title" : req.getParameter("sortBy");
        String sortOrder = req.getParameter("sortOrder") == null ? "asc" : req.getParameter("sortOrder");
        int page = req.getParameter("page") == null ? 1 : Integer.parseInt(req.getParameter("page"));
        int pageSize = req.getParameter("pageSize") == null ? 10 : Integer.parseInt(req.getParameter("pageSize"));

        resp.setContentType("application/json");

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT m.id, m.title, m.year, m.director, r.rating, ")
                .append("(SELECT GROUP_CONCAT(CONCAT('<a href=\"browse-genre.html?genre=', g.name, '\">', g.name, '</a>') ")
                .append("ORDER BY g.name SEPARATOR ', ') ")
                .append("FROM genres_in_movies gim JOIN genres g ON gim.genreId = g.id WHERE gim.movieId = m.id LIMIT 3) AS movie_genres, ")
                .append("(SELECT GROUP_CONCAT(html_name SEPARATOR ', ') FROM (")
                .append(" SELECT CONCAT('<a href=\"single-star.html?id=', s.id, '\">', s.name, '</a>') AS html_name ")
                .append(" FROM stars s JOIN stars_in_movies sim1 ON s.id = sim1.starId WHERE sim1.movieId = m.id ")
                .append(" GROUP BY s.id ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) DESC, s.name ASC LIMIT 3")
                .append(") AS limited_stars) AS movie_stars ")
                .append("FROM movies m JOIN ratings r ON m.id = r.movieId ");

        if ("*".equals(startsWith)) {
            queryBuilder.append("WHERE LEFT(m.title, 1) REGEXP '^[^a-zA-Z0-9]' ");
        } else {
            queryBuilder.append("WHERE LEFT(m.title, 1) = ? ");
        }

        queryBuilder.append("GROUP BY m.id, m.title, m.year, m.director, r.rating ");

        if (sortBy.equals("rating")) {
            queryBuilder.append("ORDER BY r.rating ").append(sortOrder.toUpperCase()).append(", m.title ASC ");
        } else {
            queryBuilder.append("ORDER BY m.title ").append(sortOrder.toUpperCase()).append(", r.rating DESC ");
        }

        queryBuilder.append("LIMIT ? OFFSET ?");

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(queryBuilder.toString())) {

            int idx = 1;
            if (!"*".equals(startsWith)) {
                ps.setString(idx++, startsWith);
            }
            ps.setInt(idx++, pageSize);
            ps.setInt(idx, (page - 1) * pageSize);

            ResultSet rs = ps.executeQuery();
            JsonArray result = new JsonArray();

            while (rs.next()) {
                JsonObject movie = new JsonObject();
                movie.addProperty("movie_id", rs.getString("id"));
                movie.addProperty("movie_title", rs.getString("title"));
                movie.addProperty("movie_year", rs.getInt("year"));
                movie.addProperty("movie_director", rs.getString("director"));
                movie.addProperty("movie_rating", rs.getFloat("rating"));
                movie.addProperty("movie_genres", rs.getString("movie_genres"));
                movie.addProperty("movie_stars", rs.getString("movie_stars"));
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
