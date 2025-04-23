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
        resp.setContentType("application/json");

        String query;

        if ("*".equals(startsWith)) {
            query = "SELECT m.id, m.title, m.year, m.director FROM movies m " +
                    "WHERE LEFT(m.title, 1) REGEXP '^[^a-zA-Z0-9]' " +
                    "ORDER BY m.title";
        } else {
            query = "SELECT m.id, m.title, m.year, m.director FROM movies m " +
                    "WHERE LEFT(m.title, 1) = ? " +
                    "ORDER BY m.title";
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            if (!"*".equals(startsWith)) {
                ps.setString(1, startsWith);
            }

            ResultSet rs = ps.executeQuery();
            JsonArray result = new JsonArray();

            while (rs.next()) {
                JsonObject movie = new JsonObject();
                movie.addProperty("id", rs.getString("id"));
                movie.addProperty("title", rs.getString("title"));
                movie.addProperty("year", rs.getInt("year"));
                movie.addProperty("director", rs.getString("director"));
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
