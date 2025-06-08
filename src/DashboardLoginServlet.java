package servlets;
import model.Employee;
import com.google.gson.JsonObject;
import org.jasypt.util.password.StrongPasswordEncryptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "DashboardLoginServlet", urlPatterns = "/_dashboard")
public class DashboardLoginServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/masterdb");
        } catch (NamingException e) {
            throw new ServletException("Unable to retrieve DataSource", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        JsonObject json = new JsonObject();
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");

        try {
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            JsonObject errorJsonObject = new JsonObject();
            errorJsonObject.addProperty("status", "fail");
            errorJsonObject.addProperty("message", "reCAPTCHA verification failed: " + e.getMessage());
            response.getWriter().write(errorJsonObject.toString());
            return;
        }
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("status", "fail");
            json.addProperty("message", "Missing email or password");
            response.getWriter().write(json.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT password, fullname FROM employees WHERE email = ?")) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String encryptedPassword = rs.getString("password");
                    String fullName = rs.getString("fullname");

                    StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();
                    if (encryptor.checkPassword(password, encryptedPassword)) {
                        // Auth success
                        HttpSession session = request.getSession();
                        session.setAttribute("employee", new Employee(email)); // or however you store them


                        json.addProperty("status", "success");
                        json.addProperty("message", "Login successful");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        json.addProperty("status", "fail");
                        json.addProperty("message", "Invalid credentials");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    json.addProperty("status", "fail");
                    json.addProperty("message", "Employee not found");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            json.addProperty("status", "error");
            json.addProperty("message", "Database error");
            e.printStackTrace();
        }

        response.getWriter().write(json.toString());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect("/fabflix/_dashboard.html"); // Or your dashboard UI file path
    }
}
