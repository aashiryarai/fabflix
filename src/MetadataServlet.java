//package servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "MetadataServlet", urlPatterns = "/api/metadata")
public class MetadataServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject result = new JsonObject();
        JsonArray tablesArray = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                JsonObject tableObj = new JsonObject();
                tableObj.addProperty("name", tableName);
                JsonArray columnsArray = new JsonArray();
                ResultSet columns = meta.getColumns(null, null, tableName, "%");
                while (columns.next()) {
                    JsonObject col = new JsonObject();
                    col.addProperty("name", columns.getString("COLUMN_NAME"));
                    col.addProperty("type", columns.getString("TYPE_NAME"));
                    columnsArray.add(col);
                }
                tableObj.add("columns", columnsArray);
                tablesArray.add(tableObj);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        result.add("tables", tablesArray);
        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }
}