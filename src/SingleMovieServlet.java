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

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
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
        String movieId = request.getParameter("id");

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + movieId);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // Construct a query with parameter represented by "?"
            String query = "SELECT * from movies m JOIN ratings r ON m.id = r.movieId WHERE m.id = ?";
            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);
            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            statement.setString(1, movieId);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            JsonObject jsonObject = new JsonObject();

            if(rs.next()) {
                jsonObject.addProperty("movie_title", rs.getString("title"));
                jsonObject.addProperty("movie_year", rs.getInt("year"));
                jsonObject.addProperty("movie_director", rs.getString("director"));
            }



            //genre list
            String genre_query = "SELECT g.name AS genreName FROM genres_in_movies gm JOIN genres g ON gm.genreId = g.id WHERE gm.movieId = ?";
            PreparedStatement g_statement = conn.prepareStatement(genre_query);
            g_statement.setString(1, movieId);
            ResultSet rs_g = g_statement.executeQuery();
            String genres = "";
            while(rs_g.next()){
                genres += rs_g.getString("genreName");
                genres += ", ";
            }
            jsonObject.addProperty("movie_genres", genres);
            rs_g.close();
            g_statement.close();

            //Listing all the Stars
            String star_query = "SELECT s.id AS starId, s.name AS starName FROM stars_in_movies sm JOIN stars s ON sm.starId = s.id WHERE sm.movieId = ?";
            PreparedStatement s_statement = conn.prepareStatement(star_query);
            s_statement.setString(1, movieId);
            ResultSet rs_s = s_statement.executeQuery();
            JsonArray starsArray = new JsonArray();

            //String stars = "";
            while(rs_s.next()){
                JsonObject starObject = new JsonObject();
                starObject.addProperty("star_id", rs_s.getString("starId"));
                starObject.addProperty("star_name", rs_s.getString("starName"));
                starsArray.add(starObject);
            }
            jsonObject.add("movie_stars", starsArray);

            rs_s.close();
            s_statement.close();


            jsonObject.addProperty("movie_rating", rs.getFloat("rating"));

            jsonArray.add(jsonObject);

            rs.close();
            statement.close();

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
