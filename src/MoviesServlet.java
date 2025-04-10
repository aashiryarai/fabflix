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
import java.sql.ResultSet;
import java.sql.Statement;


// Declaring a WebServlet called StarsServlet, which maps to url "/api/stars"
@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {

            // Declare our statement
            Statement statement = conn.createStatement();

            String query = "SELECT * FROM movies m JOIN ratings r ON m.id = r.movieId ORDER BY r.rating DESC ";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);
            //ResultSet rs1 = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                String movieId = rs.getString("id");
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("movie_title", rs.getString("title"));
                jsonObject.addProperty("movie_year", rs.getInt("year"));
                jsonObject.addProperty("movie_director", rs.getString("director"));
                /*
                jsonObject.addProperty("star_id", star_id);
                jsonObject.addProperty("star_name", star_name);
                jsonObject.addProperty("star_dob", star_dob); */
                Statement g_statement = conn.createStatement();
                String genre_query = String.format(
                        "SELECT g.name AS genreName " +
                                "FROM genres_in_movies gm " +
                                "JOIN genres g ON gm.genreId = g.id " +
                                "WHERE gm.movieId = '%s' " +
                                "LIMIT 3", movieId
                );
                ResultSet rs_g = g_statement.executeQuery(genre_query);
                String genres = "";
                while(rs_g.next()){
                    genres += rs_g.getString("genreName");
                    genres += ", ";
                }
                jsonObject.addProperty("movie_genres", genres);

                rs_g.close();
                g_statement.close();

                Statement s_statement = conn.createStatement();
                String star_query = String.format(
                        "SELECT s.name AS starName " +
                                "FROM stars_in_movies sm " +
                                "JOIN stars s ON sm.starId = s.id " +
                                "WHERE sm.movieId = '%s' " +
                                "LIMIT 3", movieId
                );
                ResultSet rs_s = s_statement.executeQuery(star_query);
                String stars = "";
                while(rs_s.next()){
                    stars += rs_s.getString("starName");
                    stars += ", ";
                }
                jsonObject.addProperty("movie_stars", stars);

                jsonArray.add(jsonObject);

                rs_s.close();
                s_statement.close();

                jsonObject.addProperty("movie_rating", rs.getFloat("rating"));

            }
            rs.close();
            statement.close();

/*
            ResultSet rs2 = statement.executeQuery(genre_query);

            String genres = "";
            JsonObject jsonObject_g = new JsonObject();
            while (rs2.next()) {

                genres += rs2.getString("genreName") + ", ";
                //jsonObject.addProperty("genre", rs2.getString("genre"));
            }

            jsonObject_g.addProperty("genres", rs2.getString("genre"));
            jsonArray.add(jsonObject_g);

            rs2.close();
            statement.close(); */

            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " results");

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }
}
