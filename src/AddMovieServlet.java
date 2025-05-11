package servlets;

import com.google.gson.JsonObject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "AddMovieServlet", urlPatterns = "/api/add-movie")
public class AddMovieServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext()
                    .lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException("Unable to retrieve DataSource", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JsonObject json = new JsonObject();

        String title     = request.getParameter("title");
        String yearStr   = request.getParameter("year");
        String director  = request.getParameter("director");
        String starName  = request.getParameter("starName");
        String genreName = request.getParameter("genreName");

        if (title == null || yearStr == null || director == null
                || starName == null || genreName == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("message", "Missing required fields");
            response.getWriter().write(json.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("{ CALL add_movie(?,?,?,?,?) }")) {

            stmt.setString(1, title);
            stmt.setInt(2, Integer.parseInt(yearStr));
            stmt.setString(3, director);
            stmt.setString(4, starName);
            stmt.setString(5, genreName);

            boolean hasResult = stmt.execute();
            String statusMsg  = "Unknown result";
            while (!hasResult && stmt.getMoreResults()) {
                hasResult = (stmt.getResultSet() != null);
            }
            if (hasResult) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        statusMsg = rs.getString("status");
                    }
                }
            }

            json.addProperty("message", statusMsg);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            json.addProperty("message", "Database error: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(json.toString());
    }
}
