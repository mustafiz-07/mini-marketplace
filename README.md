# Mini Marketplace

Mini Marketplace is a full-stack Spring Boot web application for a small marketplace platform with role-based access for buyers and sellers. It combines a server-rendered Thymeleaf frontend with REST APIs for core business operations such as user registration, product management, and order placement.

The application is designed as a practical reference project for learning and demonstrating:

- Spring MVC with Thymeleaf
- Spring Security with custom authentication
- layered backend architecture
- JPA-based persistence with PostgreSQL
- automated testing across unit, controller, and integration levels
- containerized local development and deployment

## Table of Contents

- [Project Overview](#project-overview)
- [Core Features](#core-features)
- [Technology Stack](#technology-stack)
- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [How the Application Works](#how-the-application-works)
- [Security Model](#security-model)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
- [Running with Docker](#running-with-docker)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Future Improvements](#future-improvements)
- [Review of the Previous README](#review-of-the-previous-readme)

## Project Overview

Mini Marketplace supports two primary user roles:

- `BUYER`
  Can browse products, place orders, and view personal order history.

- `SELLER`
  Can list products and monitor incoming orders through a seller dashboard.

The application uses a layered architecture where controllers handle HTTP requests, services contain business logic, repositories manage persistence, and Thymeleaf templates render the page-based UI.

## Core Features

- User registration with role selection (`BUYER` or `SELLER`)
- Form-based login using Spring Security
- BCrypt password hashing
- Public product browsing and keyword search
- Product detail page with price, stock, and seller information
- Seller-only product creation, update, and deletion flows
- Buyer order placement with stock deduction
- Buyer order history page
- Seller order dashboard with revenue and items sold
- Shared Thymeleaf fragments for layout and navigation
- Validation for form and API input
- Global exception handling for REST endpoints

## Technology Stack

- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Thymeleaf
- Thymeleaf Extras for Spring Security
- Spring Data JPA
- Spring Security
- Jakarta Validation
- PostgreSQL
- H2 Database for tests
- Maven Wrapper
- Docker and Docker Compose
- GitHub Actions
- Render

## Architecture Overview

The codebase follows a conventional Spring Boot layered design:

- `Controllers`
  Handle incoming HTTP requests.
  This project contains both page controllers (`@Controller`) and REST controllers (`@RestController`).

- `Services`
  Contain business rules such as registration, ownership checks, stock updates, and seller order aggregation.

- `Repositories`
  Use Spring Data JPA to query and persist data.

- `DTOs`
  Define request and response contracts between frontend/API and business logic.

- `Entities`
  Represent persistent models such as users, products, orders, and order items.

- `Templates`
  Thymeleaf templates render the user interface for pages like home, login, register, product listing, product details, order history, and seller orders.

## Project Structure

```text
mini-marketplace/
|-- .github/workflows/         GitHub Actions CI workflow
|-- src/main/java/com/example/minimarketplace/
|   |-- config/                MVC configuration
|   |-- controller/            Page and API controllers
|   |-- dto/                   Request and response DTOs
|   |-- exception/             Custom exceptions and global handler
|   |-- model/                 JPA entities and enums
|   |-- repository/            Spring Data repositories
|   |-- security/              Spring Security configuration
|   |-- service/               Business logic
|-- src/main/resources/
|   |-- static/                CSS, JavaScript, and image assets
|   |-- templates/             Thymeleaf HTML templates
|   |-- application.properties Runtime configuration
|-- src/test/java/             Unit, controller, and integration tests
|-- Dockerfile                 Container image definition
|-- docker-compose.yml         Local app + PostgreSQL orchestration
|-- render.yaml                Render deployment descriptor
|-- pom.xml                    Maven project definition
|-- mvnw / mvnw.cmd            Maven wrapper scripts
```

## How the Application Works

### Page-based UI

The page layer is handled mainly by `PageController`, which returns Thymeleaf templates and populates model attributes for rendering.

Key routes include:

- `/` for the home page
- `/products` for product listing and search
- `/products/{id}` for product details
- `/login` for login
- `/register` for registration
- `/order-history` for buyer order history
- `/seller/orders` for seller order monitoring

### REST API

The API layer exposes structured endpoints under:

- `/api/users`
- `/api/products`
- `/api/orders`

These endpoints support programmatic operations and some frontend interactions, while the main user experience remains server-rendered through Thymeleaf.

### Business Flow Summary

- users register through a form or API
- passwords are encoded before persistence
- sellers create products
- buyers browse available products
- buyers place orders
- product stock is reduced after successful order placement
- buyers and sellers each see role-specific order views

## Security Model

Security is configured in `src/main/java/com/example/minimarketplace/security/SecurityConfig.java`.

Important behaviors:

- authentication uses email plus password
- passwords are stored using BCrypt
- login is form-based
- public pages include home, login, register, product browsing, static assets, and selected public APIs
- seller-only routes are restricted to users with `ROLE_SELLER`
- order-related routes require an authenticated buyer or seller
- CSRF is enabled for page flows and ignored for `/api/**`

Role mapping:

- `BUYER` becomes `ROLE_BUYER`
- `SELLER` becomes `ROLE_SELLER`

## Configuration

The application expects database values through environment variables.

Required runtime variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Common optional variables:

- `SPRING_PROFILES_ACTIVE`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

Example local configuration:

```env
DB_URL=jdbc:postgresql://localhost:5432/mini_marketplace
DB_USERNAME=marketplace_user
DB_PASSWORD=marketplace_pass
SPRING_PROFILES_ACTIVE=prod
```

Helpful files already included:

- `.env.example`
- `docker-compose.yml`
- `render.yaml`

## Getting Started

### Prerequisites

- Java 21
- Git
- Docker Desktop or a local PostgreSQL installation

You do not need a globally installed Maven because the project includes the Maven Wrapper.

### Option 1: Run with local PostgreSQL

1. Start PostgreSQL locally.
2. Set the required environment variables.
3. Run the application.

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Application URL:

```text
http://localhost:8080
```

### Option 2: Run with Docker Compose

This is the easiest full local setup because it starts both the database and the application.

```bash
docker compose up --build
```

Available services:

- application: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

## Running with Docker

### Dockerfile

The project uses a multi-stage Docker build:

- Stage 1 builds the JAR with Maven and Java 21
- Stage 2 runs the JAR in a smaller Java runtime image

This keeps the final image smaller and more production-friendly.

### Docker Compose

`docker-compose.yml` defines two services:

- `db`
  PostgreSQL 16 with health checks and a persistent volume

- `app`
  Spring Boot application built from the local `Dockerfile`

The app connects to the database using the Compose service hostname `db`.

## Testing

The repository includes several types of automated tests:

- service unit tests
- controller tests using `MockMvc`
- API integration tests
- Thymeleaf page rendering integration tests
- application context startup test

Run all tests:

macOS/Linux:

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Run a single test class:

macOS/Linux:

```bash
./mvnw -Dtest=PageTemplateIntegrationTest test
```

Windows PowerShell:

```powershell
.\mvnw.cmd -Dtest=PageTemplateIntegrationTest test
```

Testing notes:

- most tests use H2 in-memory database
- page rendering tests verify real Thymeleaf output
- CI provisions PostgreSQL to keep the pipeline close to production behavior

## CI/CD

GitHub Actions workflow:

- `.github/workflows/ci.yml`

The CI pipeline:

- runs on pull requests
- runs on pushes to `main`, `develop`, and `feature/**`
- can be triggered manually via `workflow_dispatch`
- provisions PostgreSQL as a service container
- installs Java 21
- runs `./mvnw -B clean verify`
- uploads the packaged JAR as an artifact on success

This gives the team continuous build verification for every major change path.

## Deployment

The repository includes `render.yaml` for Docker-based deployment on Render.

Deployment characteristics:

- build source is the project `Dockerfile`
- `autoDeploy` is enabled
- database values are provided via environment variables
- `SPRING_PROFILES_ACTIVE=prod` can be used for production behavior

The same container image approach can also be used on any Docker-compatible platform.

## Troubleshooting

### Datasource or startup failure

Check that these variables are set correctly:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

### `mvn` is not recognized

Use the Maven Wrapper:

- `./mvnw` on macOS/Linux
- `.\mvnw.cmd` on Windows

### Docker app cannot connect to PostgreSQL

Verify that:

- the `db` container is healthy
- port `5432` is not already occupied
- the application uses `db` as the database host inside Docker

### Page tests and CSRF-related errors

Some Thymeleaf templates depend on Spring Security request attributes such as `_csrf`. If you are writing tests for secured pages, make sure the test setup matches the security behavior you want to verify.

## Future Improvements

- Add pagination for products and orders
- Add image upload support
- Add seller-facing edit forms in the UI
- Add API documentation with OpenAPI
- Add profile-specific application config files
- Add observability and health endpoints
- Expand authorization checks around API ownership and write operations

