import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "SessionServlet", urlPatterns = "/api/session")
public class SessionServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        JsonObject json = new JsonObject();

        if (session != null) {
            json.addProperty("isCustomer", session.getAttribute("user") != null);
            json.addProperty("isEmployee", session.getAttribute("employee") != null);
        } else {
            json.addProperty("isCustomer", false);
            json.addProperty("isEmployee", false);
        }

        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}