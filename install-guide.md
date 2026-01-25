# Installation Guide: Lost and Found

This document provides the necessary instructions to set up, build, and run the **Lost and Found** application—a full-stack solution designed for the Georgia Tech community to manage and locate found items using location-based search and real-time chat.

---

## 1. Pre-requisites
Before beginning the installation, ensure your system meets the following hardware and software requirements.

### Hardware Requirements
* **Processor:** Quad-core 2.0 GHz or higher.
* **Memory (RAM):** Minimum 8 GB (16 GB recommended for optimal Docker performance).
* **Storage:** 2 GB of available disk space.

### Software Requirements
* **Operating System:** Windows 10/11 (with WSL2), macOS, or a modern Linux distribution.
* **Docker Desktop:** Required for containerized deployment. [Download Docker](https://www.docker.com/products/docker-desktop/)
* **Node.js 22+:** Required if running the frontend locally. [Download Node.js](https://nodejs.org/)
* **Java Development Kit (JDK) 21:** Required if running the backend locally. [Download OpenJDK 21](https://jdk.java.net/21/)
* **Git:** To clone the project repository. [Download Git](https://git-scm.com/)

---

## 2. Dependent Libraries
The following third-party libraries and software are required for the application to function:

* **PostGIS:** A spatial database extender for PostgreSQL (included automatically in the Docker configuration). [PostGIS Official Site](https://postgis.net/)
* **Maven:** Build automation tool for Java (the project includes a Maven Wrapper `./mvnw`, so a standalone installation is optional). [Maven Official Site](https://maven.apache.org/)
* **Frontend Dependencies:** Managed via npm. To install these, navigate to the `frontend/` directory and run:
    ```bash
    npm install
    ```

---

## 3. Download Instructions
Access the project source code by cloning the repository using Git:

```bash
git clone https://github.com/sman11235/cs3803-lost-and-found.git
cd cs3803-lost-and-found
```

---

## 4. Build Instructions
The application is provided as raw source code and must be built before the first execution.

### Building via Docker (Recommended)
This command builds the frontend, backend, and database containers simultaneously:
```bash
docker compose build
```

### Manual Build (Local Development)
1.  **Backend:** Navigate to `backend/app/` and package the application:
    ```bash
    ./mvnw clean package
    ```
2.  **Frontend:** Navigate to `frontend/` and build the production assets:
    ```bash
    npm run build
    ```

---

## 5. Installation of Actual Application
After building, you must configure the environment variables and ensure the directory structure is correct.

### Required Directories
Ensure the following structure is intact:
* `backend/app/`: Spring Boot source and environment templates.
* `frontend/`: React/Vite source code.
* `postgres/`: Database initialization and pgAdmin configuration scripts.

### Configuration Steps
1.  **Backend Configuration:**
    Navigate to `backend/app/` and create an `.env` file from the template:
    ```bash
    cp .env.template .env
    ```
    *Edit the `.env` file to include your specific `JWT_HMAC_SECRET_KEY` and database credentials.*

2.  **Frontend Configuration:**
    If you wish to use Google Maps features, navigate to `frontend/` and create an `.env` file:
    ```bash
    echo "VITE_GOOGLE_MAPS_API_KEY=your_actual_key_here" > .env
    ```

---

## 6. Run Instructions
To execute the software, follow the steps below:

### Using Docker Compose (Standard Run)
Run the following command from the repository root:
```bash
docker compose up
```
**Access Points:**
* **Frontend UI:** [http://localhost:5173](http://localhost:5173)
* **Backend API:** [http://localhost:8080](http://localhost:8080)
* **pgAdmin:** [http://localhost:5050](http://localhost:5050)

### Local Development Mode
1.  **Database:** Ensure a PostgreSQL instance with PostGIS is running on port `5432`.
2.  **Backend:** In `backend/app/`, run `./mvnw spring-boot:run`.
3.  **Frontend:** In `frontend/`, run `npm run dev`.

---

## 7. Troubleshooting
Common errors and their corrective actions:

| Error | Cause | Corrective Action |
| :--- | :--- | :--- |
| `Port 8080 is already in use` | Another process is using the backend port. | Stop the conflicting process or change the port in `application.properties`. |
| `PostGIS extension not found` | Local DB was not initialized with spatial support. | Run `CREATE EXTENSION postgis;` in your SQL console or use the Docker setup. |
| `JWT secret key error` | Missing or invalid key in `.env`. | Verify `backend/app/.env` contains a valid `JWT_HMAC_SECRET_KEY`. |
| `Google Maps UI limited` | Missing or invalid VITE API key. | Ensure the key is correctly set in `frontend/.env` and has "Maps JavaScript API" enabled. |
| `Node version mismatch` | Using an older version of Node.js. | Update to Node.js 22+ (recommend using `nvm` to manage versions). |
