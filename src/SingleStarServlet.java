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

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/slavedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String starId = request.getParameter("id");

        request.getServletContext().log("Getting star id: " + starId);

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("SELECT s.id, s.name, s.birthYear, ")
                    .append("(SELECT GROUP_CONCAT(")
                    .append("    CONCAT('<a href=\"single-movie.html?id=', m.id, '\">', m.title, '</a>') ")
                    .append("    ORDER BY m.year DESC, m.title ASC SEPARATOR ', ') ")
                    .append(" FROM stars_in_movies sim ")
                    .append(" JOIN movies m ON sim.movieId = m.id ")
                    .append(" WHERE sim.starId = s.id) AS movies ")
                    .append("FROM stars s ")
                    .append("WHERE s.id = ? ");

            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());
            statement.setString(1, starId);

            ResultSet rs = statement.executeQuery();
            JsonArray jsonArray = new JsonArray();

            if (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("star_id", rs.getString("id"));
                jsonObject.addProperty("star_name", rs.getString("name"));
                jsonObject.addProperty("year_of_birth", rs.getString("birthYear") == null ? "N/A" : rs.getString("birthYear"));
                jsonObject.addProperty("star_movies", rs.getString("movies") == null ? "" : rs.getString("movies"));
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
