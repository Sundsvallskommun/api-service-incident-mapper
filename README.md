# IncidentMapper

_Automatically creates and manages Jira issues based on incoming incidents, maintaining bi-directional synchronization of status updates between Jira and POB (Point of Business). Ensures seamless incident tracking and resolution workflow across systems._

## Getting Started

### Prerequisites

- **Java 21 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Sundsvallskommun/api-service-incident-mapper.git
   cd api-service-incident-mapper
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#Configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible. See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

## Dependencies

This microservice depends on the following services:

- **Jira**
  - **Purpose:** The services creates jira issues based on the incoming incidents and syncs their progress
  - **External URL:** [Atlassian Jira](https://www.atlassian.com/software/jira)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Messaging**
  - **Purpose:** Used to send messages if something goes wrong with handling incidents.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-messaging)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **POB**
  - **Purpose:** POB is the case management system where the system finds incidents and syncs back status from jira.
  - **External URL:** [Serviceaide POB](https://www.serviceaide.com/products/pob)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

Refer to the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X POST http://localhost:8080/2281/incidents \ 
  -H "Content-Type: application/json" \
  -d '{"incidentKey": "12345"}'
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in `application.yml`.

### Key Configuration Parameters

- **Server Port:**

  ```yaml
  server:
    port: 8080
  ```
- **Database Settings:**

  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/your_database
      username: your_db_username
      password: your_db_password
  ```
- **External Service URLs:**

  ```yaml
  integration:
    messaging:
      url: http://messaging.url
      channel: the-channel
      token: the-token
    pob:
      url: http://pob.url
      apiKey: some-key
    jira:
      password: some-password
      username: some-username
      url: http://jira.url
      projectKey: PROJECT-KEY
  spring:
    security:
      oauth2:
        client:
          provider:
            messaging:
              token-uri: http://token.url
          registration:
            messaging:
              client-id: the-client-id
              client-secret: the-client-secret
  ```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is disabled by default so you will have to enable it to automatically populate the database schema upon application startup.

```yaml
spring:
  flyway:
    enabled: true
```

- **No additional setup is required** for database initialization, as long as the database connection settings are correctly configured.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-incident-mapper&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-incident-mapper)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-incident-mapper&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-incident-mapper)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-incident-mapper&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-incident-mapper)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-incident-mapper&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-incident-mapper)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-incident-mapper&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-incident-mapper)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-incident-mapper&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-incident-mapper)

---

© 2024 Sundsvalls kommun
