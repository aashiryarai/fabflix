import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedbexample");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String id = request.getParameter("id");

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            //whatever is already available in stars table
            String query = "SELECT * FROM stars WHERE id = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            JsonObject jsonObject = new JsonObject();


            if(rs.next()){
                jsonObject.addProperty("star_name", rs.getString("name"));
                jsonObject.addProperty("year_of_birth", rs.getString("birthYear"));
            }

            rs.close();
            statement.close();

            //movie query separate
           /* String movie_query = "SELECT m.id AS movieId, m.title AS movieName FROM stars_in_movies sm JOIN movies m ON sm.movieId = m.id WHERE sm.starId = ?";
            PreparedStatement m_statement = conn.prepareStatement(movie_query);
            m_statement.setString(1, id);
            ResultSet rs_m = m_statement.executeQuery();

            JsonArray moviesArray = new JsonArray();

            while (rs_m.next()) {

                JsonObject movieObject = new JsonObject();
                movieObject.addProperty("movie_id", rs_m.getString("movieId"));
                movieObject.addProperty("movie_name", rs_m.getString("movieName"));
                moviesArray.add(movieObject);
            }
            jsonObject.add("movies", moviesArray);

            rs_m.close();
            m_statement.close();

            */

            jsonArray.add(jsonObject);

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

}