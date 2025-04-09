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
import java.util.HashSet;

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
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

            // Construct a query with parameter represented by "?"
            String query = "SELECT * FROM movies AS m, ratings AS r, " +
                    "stars_in_movies AS sim, stars AS s, genres_in_movies AS gm, " +
                    "genres AS g " +
                    "WHERE m.id = ? AND m.id = r.movieId " +
                    "AND m.id = sim.movieId " +
                    "AND sim.starId = s.id " +
                    "AND m.id = gm.movieId " +
                    "AND gm.genreId = g.id";

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            statement.setString(1, id);

            // Perform the query
            ResultSet rs = statement.executeQuery();

            //JsonArray jsonArray = new JsonArray();
            JsonObject jsonObject = new JsonObject();
            JsonArray genresArray = new JsonArray();
            JsonArray starsArray = new JsonArray();

            HashSet<String> genreSet = new HashSet<>();
            HashSet<String> starSet = new HashSet<>();

            //boolean initialized = false;
            // Iterate through each row of rs
            while (rs.next()) {
                jsonObject.addProperty("movie_id", rs.getString("id"));
                jsonObject.addProperty("movie_title", rs.getString("title"));
                jsonObject.addProperty("movie_year", rs.getString("year"));
                jsonObject.addProperty("movie_director", rs.getString("director"));
                jsonObject.addProperty("movie_rating", rs.getString("rating"));

                String genre = rs.getString("g.name");
                if (genreSet.add(genre)) {
                    genresArray.add(genre);
                }

                String starId = rs.getString("s.id");
                if (starSet.add(starId)) {
                    JsonObject starObject = new JsonObject();
                    starObject.addProperty("star_id", starId);
                    starObject.addProperty("star_name", rs.getString("s.name"));
                    starsArray.add(starObject);
                }
            }
            jsonObject.add("genres", genresArray);
            jsonObject.add("stars", starsArray);

            JsonArray resultArray = new JsonArray();
            resultArray.add(jsonObject);

            rs.close();
            statement.close();

            // Write JSON string to output
            //out.write(jsonArray.toString());
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
