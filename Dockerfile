# Stage 1: Build with Maven
FROM maven:3.8.5-openjdk-11-slim AS builder

# Set working directory in build container
WORKDIR /app

# Copy project into build container
COPY . .

# Build WAR file with Maven
RUN mvn clean package

# Stage 2: Run with Tomcat 9
FROM tomcat:9-jdk11

# Clean default webapps (optional)
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy WAR file from Maven build stage
COPY --from=builder /app/target/fabflix.war /usr/local/tomcat/webapps/fabflix.war

# Expose port 8080
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]
