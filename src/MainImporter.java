import java.sql.Connection;
import java.sql.DriverManager;

public class MainImporter {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/moviedb", "root", "Akash13579!");

            conn.setAutoCommit(false); // turn off autocommit for performance

            new MainsParser(conn).run();
            new ActorsParser(conn).run();
            new CastsParser(conn).run();

            conn.commit(); // finalize changes
            System.out.println("Data successfully imported");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
