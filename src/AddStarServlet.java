package servlets;

import com.google.gson.JsonObject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

@WebServlet(name = "AddStarServlet", urlPatterns = "/api/add-star")
public class AddStarServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        String birthYearStr = request.getParameter("birthYear");

        System.out.println("DEBUG: Received name = " + name);
        System.out.println("DEBUG: Received birthYear = " + birthYearStr);

        response.setContentType("application/json");
        JsonObject jsonObject = new JsonObject();

        try (Connection conn = dataSource.getConnection()) {
            String query = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            String newId = "nm" + UUID.randomUUID().toString().substring(0, 7);

            System.out.println("DEBUG: Generated ID = " + newId);
            System.out.println("DEBUG: Preparing to execute insert...");

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, newId);
                ps.setString(2, name);
                if (birthYearStr == null || birthYearStr.isEmpty()) {
                    ps.setNull(3, java.sql.Types.INTEGER);
                    System.out.println("DEBUG: Birth year set to NULL");
                } else {
                    ps.setInt(3, Integer.parseInt(birthYearStr));
                }

                int rows = ps.executeUpdate();
                System.out.println("DEBUG: Insert result = " + rows);
                jsonObject.addProperty("message", "Star added successfully!");
                jsonObject.addProperty("id", newId);
            }
        } catch (Exception e) {
            e.printStackTrace(); // show full stack trace in server log
            jsonObject.addProperty("message", "Error: " + e.getMessage());
        }

        response.getWriter().write(jsonObject.toString());
    }
}
