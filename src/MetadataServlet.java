package servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "MetadataServlet", urlPatterns = "/api/metadata")
public class MetadataServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            dataSource = (DataSource)
                    new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        JsonObject result    = new JsonObject();
        JsonArray  tablesArr = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // either hard‐code your DB name:
            String myDb = "moviedb";

            try (ResultSet tables = meta.getTables(myDb, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");

                    JsonObject tableObj = new JsonObject();
                    tableObj.addProperty("name", tableName);

                    JsonArray colsArr = new JsonArray();
                    try (ResultSet cols = meta.getColumns(myDb, null, tableName, "%")) {
                        while (cols.next()) {
                            JsonObject col = new JsonObject();
                            col.addProperty("name", cols.getString("COLUMN_NAME"));
                            col.addProperty("type", cols.getString("TYPE_NAME"));
                            colsArr.add(col);
                        }
                    }

                    tableObj.add("columns", colsArr);
                    tablesArr.add(tableObj);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        result.add("tables", tablesArr);
        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }

}
