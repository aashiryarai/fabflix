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

@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String searchType = request.getParameter("searchType");
        boolean isFulltext = "fulltext".equalsIgnoreCase(searchType);

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");

        String sortBy = request.getParameter("sortBy") == null ? "title" : request.getParameter("sortBy");
        String sortOrder = request.getParameter("sortOrder") == null ? "asc" : request.getParameter("sortOrder");
        int page = request.getParameter("page") == null ? 1 : Integer.parseInt(request.getParameter("page"));
        int pageSize = request.getParameter("pageSize") == null ? 10 : Integer.parseInt(request.getParameter("pageSize"));

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("SELECT m.id, m.title, m.year, m.director, r.rating, ")
                    .append("(SELECT GROUP_CONCAT(CONCAT('<a href=\"browse-genre.html?genre=', g.name, '\">', g.name, '</a>') ORDER BY g.name SEPARATOR ', ') ")
                    .append(" FROM genres_in_movies gim JOIN genres g ON gim.genreId = g.id WHERE gim.movieId = m.id LIMIT 3) AS genres, ")
                    .append("(SELECT GROUP_CONCAT(html_name SEPARATOR ', ') FROM (SELECT CONCAT('<a href=\"single-star.html?id=', s.id, '\">', s.name, '</a>') AS html_name ")
                    .append(" FROM stars s JOIN stars_in_movies sim1 ON s.id = sim1.starId WHERE sim1.movieId = m.id GROUP BY s.id ")
                    .append(" ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) DESC, s.name ASC LIMIT 3) AS limited_stars) AS stars ")
                    .append("FROM movies m ")
                    .append("LEFT JOIN ratings r ON m.id = r.movieId ");

            if (star != null && !star.isEmpty()) {
                queryBuilder.append("JOIN stars_in_movies sim ON m.id = sim.movieId ")
                        .append("JOIN stars s ON sim.starId = s.id ");
            }

            queryBuilder.append("WHERE 1=1 ");

            String[] titleTokens = null;
            if (title != null && !title.trim().isEmpty()) {
                if (isFulltext) {
                    titleTokens = title.trim().toLowerCase().split("\\s+");

                    // Defensive: if no valid tokens, respond early
                    if (titleTokens.length == 0) {
                        response.setStatus(400);
                        out.write("{\"errorMessage\": \"Empty full-text query.\"}");
                        return;
                    }

                    queryBuilder.append("AND MATCH(m.title) AGAINST (? IN BOOLEAN MODE) ");
                } else {
                    queryBuilder.append("AND m.title LIKE ? ");
                }
            }

            if (year != null && !year.isEmpty()) {
                queryBuilder.append("AND m.year = ? ");
            }
            if (director != null && !director.isEmpty()) {
                queryBuilder.append("AND m.director LIKE ? ");
            }
            if (star != null && !star.isEmpty()) {
                queryBuilder.append("AND s.name LIKE ? ");
            }

            queryBuilder.append("GROUP BY m.id, m.title, m.year, m.director, r.rating ");

            if (sortBy.equals("rating")) {
                queryBuilder.append("ORDER BY r.rating ").append(sortOrder.toUpperCase()).append(", m.title ASC ");
            } else {
                queryBuilder.append("ORDER BY m.title ").append(sortOrder.toUpperCase()).append(", r.rating DESC ");
            }

            queryBuilder.append("LIMIT ? OFFSET ?");

            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());
            int index = 1;

            if (title != null && !title.isEmpty()) {
                if (isFulltext && titleTokens != null && titleTokens.length > 0) {
                    StringBuilder fulltextQuery = new StringBuilder();
                    for (String token : titleTokens) {
                        fulltextQuery.append("+").append(token).append("* ");
                    }
                    statement.setString(index++, fulltextQuery.toString().trim());
                }else {
                    statement.setString(index++, "%" + title + "%");
                }
            }

            if (year != null && !year.isEmpty()) {
                statement.setInt(index++, Integer.parseInt(year));
            }
            if (director != null && !director.isEmpty()) {
                statement.setString(index++, "%" + director + "%");
            }
            if (star != null && !star.isEmpty()) {
                statement.setString(index++, "%" + star + "%");
            }

            statement.setInt(index++, pageSize);
            statement.setInt(index++, (page - 1) * pageSize);

            ResultSet rs = statement.executeQuery();
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", rs.getString("id"));
                jsonObject.addProperty("movie_title", rs.getString("title"));
                jsonObject.addProperty("movie_year", rs.getInt("year"));
                jsonObject.addProperty("movie_director", rs.getString("director"));
                jsonObject.addProperty("movie_rating", rs.getFloat("rating"));
                jsonObject.addProperty("movie_genres", rs.getString("genres") != null ? rs.getString("genres") : "");
                jsonObject.addProperty("movie_stars", rs.getString("stars") != null ? rs.getString("stars") : "");
                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();

            request.getServletContext().log("getting " + jsonArray.size() + " results");
            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
