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
import javax.servlet.ServletException;


@WebServlet("/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private DataSource dataSource;


    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();


        String query = request.getParameter("query");
        JsonObject result = new JsonObject();
        JsonArray suggestions = new JsonArray();


        if (query == null || query.trim().length() < 3) {
            result.add("suggestions", suggestions);
            out.write(result.toString());
            return;
        }


        query = query.trim().toLowerCase(); // Normalize input
        String[] tokens = query.split("\\s+");
        StringBuilder fullText = new StringBuilder();
        for (String token : tokens) {
            token = token.replaceAll("[^a-zA-Z0-9]", ""); // Remove special characters
            if (!token.isEmpty()) {
                fullText.append("+").append(token).append("* ");
            }
        }


        System.out.println("Autocomplete fulltext query: " + fullText.toString().trim());


        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT id, title, year FROM movies WHERE MATCH(title) AGAINST (? IN BOOLEAN MODE) LIMIT 10";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, fullText.toString().trim());


            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JsonObject suggestion = new JsonObject();
                suggestion.addProperty("value", rs.getString("title") + " (" + rs.getString("year") + ")");
                suggestion.addProperty("data", rs.getString("id")); // used for redirect
                suggestions.add(suggestion);
            }


            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
            // Optional: respond with empty suggestions even on failure
            result.add("suggestions", suggestions);
        }


        result.add("suggestions", suggestions);
        out.write(result.toString());
    }
}



