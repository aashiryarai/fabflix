- # General
    - #### Team#: aa
    
    - #### Names: Aashirya Rai, Amratha Rao
    
    - #### Project 5 Video Demo Link: https://drive.google.com/file/d/1KL3l6NilROZQ51FP4ybPDr04tX5mf-lQ/view?usp=sharing

    - #### Collaborations and Work Distribution: Full text search: amratha, autocomplete: amratha, connection pooling: aashirya, master and slave: aashirya, load balancing: aashirya 

- # Connection Pooling
    - #### Include the filename/path of all code/configuration files in GitHub of using JDBC Connection Pooling.
      servlets that use retrieve connection from pool: (.java)
      AddMovieServlet
      AddStarServlet
      DashboardLoginServlet
      LoginServlet
      MoviesServlet
      AutocompleteServlet
      BrowseGenreServlet
      GenresServlet
      MetadataServlet
      PaymentServlet
      SingleMovieServlet
      SingleStarServlet
      TitlesServlet
      WebContent/META-INF/context.xml: Defines the `jdbc/moviedb` resource with Tomcat's JDBC connection pool.
      WebContent/WEB-INF/web.xml: Declares the `<resource-ref>` so servlets can access the pooled DataSource.
    - #### Explain how Connection Pooling is utilized in the Fabflix code.
      Fabflix uses Tomcat's built-in connection pooling defined in the context.xml file under the META-INF directory. It declares a <Resource> element with attributes like maxTotal,          maxIdle, maxWaitMillis, and the JDBC URL. Each servlet retrieves a connection using: 
      DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
      Connection conn = ds.getConnection();
      conn.close() returns the connection to the pool for reuse. This minimizes the overhead of repeatedly opening/closing physical connections and ensures efficient handling of     
      multiple concurrent requests.
    - #### Explain how Connection Pooling works with two backend SQL.
      Fabflix uses two MySQL databases: the master for writes and the slave for reads. Tomcat manages connection pooling for both independently by defining two separate <Resource>     
      entries in context.xml, each corresponding to a different backend:
      <Resource name="jdbc/masterdb"
          type="javax.sql.DataSource"
          username="fabflix"
          password="..."
          driverClassName="com.mysql.cj.jdbc.Driver"
          url="jdbc:mysql://MASTER_IP:3306/moviedb"
          maxTotal="100" maxIdle="30" maxWaitMillis="10000"/>
      <Resource name="jdbc/slavedb"
          type="javax.sql.DataSource"
          username="fabflix"
          password="..."
          driverClassName="com.mysql.cj.jdbc.Driver"
          url="jdbc:mysql://SLAVE_IP:3306/moviedb"
          maxTotal="100" maxIdle="30" maxWaitMillis="10000"/>
      Within servlets, connect to each pool independently using JNDI:
      DataSource masterDS = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/masterdb");
      DataSource slaveDS = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/slavedb");
      Each DataSource has its own pool, managed by Tomcat. The master pool handles write operations (insert/update/delete), while the slave pool handles read operations (select). This 
      separation improves scalability by distributing the workload between the two databases, and the connection pool ensures efficient reuse of MySQL connections.

- # Master/Slave
    - #### Include the filename/path of all code/configuration files in GitHub of routing queries to Master/Slave SQL.
      WebContent/META-INF/context.xml: Defines the connection pools for both master and slave databases.
      Contains two <Resource> tags with different JNDI names:
      <Resource name="jdbc/masterdb" ... />
      <Resource name="jdbc/slavedb" ... />
      Also the context.xml and servlets as mentioned before.

    - #### How read/write requests were routed to Master/Slave SQL?
      In the Fabflix implementation, read requests and write requests were routed differently based on the servlet’s function: For read-only operations like browsing movies, searching,       or viewing metadata, the servlets used a JNDI lookup to the slave database:
      Context initCtx = new InitialContext();
      Context envCtx = (Context) initCtx.lookup("java:comp/env");
      DataSource ds = (DataSource) envCtx.lookup("jdbc/slavedb");
      Connection conn = ds.getConnection();
      These connections handled all SELECT queries, helping offload the master database and improve scalability.
      For write operations (e.g., inserting stars, adding new movies), the servlets directly connected to the master database by looking up the jdbc/masterdb JNDI resource:
      Context initCtx = new InitialContext();
      Context envCtx = (Context) initCtx.lookup("java:comp/env");
      DataSource ds = (DataSource) envCtx.lookup("jdbc/masterdb");
      Connection conn = ds.getConnection();
      This approach ensures all INSERT, UPDATE, or DELETE statements are routed to the master. The connection to master or slave was handled manually per servlet, based on whether the 
      purpose was to read or write.
