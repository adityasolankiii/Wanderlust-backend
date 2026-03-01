# Wanderlust Property Booking Backend

This is the backend service for the Wanderlust Property Booking platform, built with Java Spring Boot.

## Features

- User registration, login, and JWT authentication
- Password reset and email verification
- User profile management
- Property listing creation, update, deletion, and search/filter
- Image upload with Cloudinary
- Booking creation, cancellation, and history
- Review system for listings
- Admin dashboard endpoints
- Payment gateway integration
- GDPR compliance (data export and deletion)
- OAuth2 social login support
- Role management and advanced search filters
- API rate limiting, error handling, and logging
- Swagger API documentation

## Getting Started

### Prerequisites

- Java 17+
- Maven
- MySQL or PostgreSQL (or your preferred DB)

### Setup

1. Clone the repository:
   ```sh
   git clone <repo-url>
   cd wanderlust
   ```
2. Configure your database and environment variables in `src/main/resources/application.properties`.
3. Build and run the application:
   ```sh
   ./mvnw spring-boot:run
   ```

### API Documentation

- Swagger UI available at `/swagger-ui.html` after running the app.

## Folder Structure

- `src/main/java/com/wanderlust/wanderlust/` - Main source code
- `controller/` - REST controllers
- `service/` - Business logic
- `repository/` - Data access
- `entity/` - JPA entities
- `dto/` - Data transfer objects
- `security/` - Security and authentication
- `config/` - Configuration files
- `resources/` - Application properties and static resources
