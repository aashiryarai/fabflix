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

@WebServlet(name = "GenresServlet", urlPatterns = "/api/genres")
public class GenresServlet extends HttpServlet {
    private DataSource ds;

    public void init(ServletConfig config) {
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/slavedb");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        String query = "SELECT name FROM genres ORDER BY name";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            JsonArray genres = new JsonArray();
            while (rs.next()) {
                genres.add(rs.getString("name"));
            }

            resp.getWriter().write(genres.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
}
