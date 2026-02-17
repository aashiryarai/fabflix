# Fabflix (CS 122B – Spring 2025)

Fabflix is a full-stack movie browsing web application built throughout CS 122B.
The project progressively evolves from a basic MySQL-backed web app into a scalable, distributed system with replication, connection pooling, full-text search, load balancing, and Kubernetes-based container orchestration.

---

## Project Overview

Fabflix allows users to:

- Browse movies by genre or title
- Search movies with full-text search
- Use autocomplete suggestions
- View single movie and star pages
- Add movies and stars (admin dashboard)
- Complete checkout/payment flow
- Log in securely via HTTPS
- Scale across multiple database and backend instances

The application was developed incrementally across five major milestones.

---

## Tech Stack

### Backend
- Java (Servlets)
- JDBC
- Apache Tomcat
- MySQL
- Stored Procedures
- XML Parsing

### Frontend
- HTML
- CSS
- JavaScript
- AJAX

### Infrastructure
- AWS EC2
- MySQL Master/Slave Replication
- Apache Load Balancer
- JDBC Connection Pooling
- Docker (for containerization)
- Kubernetes (K8s) Clusters
---

## Project Breakdown

### Project 1 – Database & Basic Setup : https://drive.google.com/file/d/18GRKODaSeMOs3exX-9hmG4fvV6NbGXnF/view?usp=sharing
- MySQL schema creation
- JDBC connectivity
- Tomcat deployment
- AWS EC2 setup
- Initial Fabflix skeleton

### Project 2 – Website Development : https://drive.google.com/file/d/1osgZFJxLIWlvop4jUV536aUyqZgDc2EE/view?usp=sharing
- Movie list page
- Single movie page
- Single star page
- Pagination
- Sorting
- AJAX integration

### Project 3 – Security & Backend Enhancements : https://drive.google.com/file/d/11HzaqortF6HSK5rwHAjCablt3uiQW3RF/view?usp=sharing
- HTTPS configuration
- reCAPTCHA
- Stored procedures
- XML parsing for movie metadata
- Secure admin dashboard

### Project 4 – Search & Advanced Features : https://drive.google.com/file/d/1KL3l6NilROZQ51FP4ybPDr04tX5mf-lQ/view?usp=sharing
- Full-text search
- Autocomplete
- Fuzzy search
- Android client integration

### Project 5 – Scaling Fabflix : https://drive.google.com/file/d/1pnly1iIX02EPpW3l62EcVe4Xe7LZQybU/view?usp=sharing
- MySQL Master/Slave replication
- Read/write query routing
- JDBC connection pooling
- Load balancing
- Horizontal scaling across backend instances
- Deployment on Kubernetes (K8s) cluster
  
---

## Key Features

### Authentication & Security
- HTTPS-enabled deployment
- Secure employee login
- reCAPTCHA integration
- Session management

### Search
- Full-text search using MySQL indexing
- Autocomplete suggestions
- Pagination and filtering

### Admin Dashboard
- Add stars
- Add movies
- Insert metadata via stored procedures

### Scalability
- Separate read and write databases
- Slave database handles SELECT queries
- Master database handles INSERT/UPDATE/DELETE
- Connection pooling via Tomcat
- Apache load balancing with sticky sessions

---
## Repository Structure (High-Level)
WebContent/
META-INF/
WEB-INF/
js/
css/

src/
servlets/
utils/

Dockerfile

## Languages Used
- Java (~57%)
- JavaScript (~22%)
- HTML (~21%)
- Dockerfile (~0.4%)
Dockerfile

