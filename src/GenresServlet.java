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
import java.sql.Statement;

@WebServlet(name="GenresServlet", urlPatterns="/api/genres")
public class GenresServlet extends HttpServlet {
    private DataSource ds;
    public void init(ServletConfig c) {
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new RuntimeException("Failed to init Datasource", e);
        }
    }

    protected void doGet(HttpServletRequest req,HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM genres ORDER BY name")) {
            JsonArray arr = new JsonArray();
            while (rs.next()) arr.add(rs.getString("name"));
            resp.getWriter().write(arr.toString());
        } catch (Exception e) {
            resp.setStatus(500);
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", e.getMessage());
            resp.getWriter().write(errorJson.toString());
        }
    }
}

