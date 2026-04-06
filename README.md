# Multi-Tenant SaaS Platform

A production-grade Multi-Tenant SaaS Platform built with **Spring Boot**, **MongoDB**, and **JWT Authentication**.

## 🔷 Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Language |
| Spring Boot 3.2 | Framework |
| Spring Security | JWT-based Authentication & RBAC |
| MongoDB | Database (shared database, tenant isolation via `tenantId`) |
| Docker | Containerization |
| Swagger/OpenAPI | API Documentation |

## 🔷 Architecture

### Multi-Tenancy Strategy
- **Shared database** with `tenantId` field in all collections
- `TenantContext` using `ThreadLocal` for request-scoped tenant resolution
- `X-Tenant-ID` HTTP header extracted via `HandlerInterceptor`
- All repository queries are tenant-aware — **zero cross-tenant data leakage**

### Security (RBAC)
- JWT-based stateless authentication
- Three roles: `SUPER_ADMIN`, `TENANT_ADMIN`, `USER`
- Method-level authorization via `@PreAuthorize`
- Tenant ID in JWT validated against `X-Tenant-ID` header
- **USER role** can view products and **sell products** (decrements stock)
- Products auto-deactivate when `productCount` reaches 0

## 🔷 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development)
- Maven 3.9+

### Run with Docker Compose
```bash
docker-compose up --build
```

This starts:
- **App** on `http://localhost:8080`
- **MongoDB** on `localhost:27017`

### Run Locally
```bash
# Start MongoDB (via Docker)
docker-compose up mongo -d

# Run the application
mvn spring-boot:run
```

## 🔷 API Endpoints

### Authentication
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/auth/register` | Register a new user | Public* |
| POST | `/auth/login` | Login and get JWT | Public* |

*Requires `X-Tenant-ID` header

### Tenants
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/tenants` | Create a tenant | Public |
| GET | `/tenants` | List all tenants | Public |
| GET | `/tenants/{id}` | Get tenant by ID | Public |

### Users
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/users` | Create a user | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/users` | List tenant users | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/users/{id}` | Get user by ID | Any authenticated |

### Products
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/products` | Create a product (with `productCount`) | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/products` | List tenant products | Any authenticated |
| GET | `/products/{id}` | Get product by ID | Any authenticated |
| PUT | `/products/{id}` | Update a product (including `productCount`) | SUPER_ADMIN, TENANT_ADMIN |
| DELETE | `/products/{id}` | Delete a product | SUPER_ADMIN, TENANT_ADMIN |
| POST | `/products/{id}/sell?quantity=1` | Sell a product (decreases `productCount`) | Any authenticated (USER, TENANT_ADMIN, SUPER_ADMIN) |

### Usage
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/usage/update` | Update usage metrics | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/usage` | Get usage data | SUPER_ADMIN, TENANT_ADMIN |

### Billing
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/billing/invoice` | Get all invoices | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/billing/invoice/{month}` | Get invoice for month | SUPER_ADMIN, TENANT_ADMIN |
| POST | `/billing/generate/{month}` | Generate invoice | SUPER_ADMIN, TENANT_ADMIN |

## 🔷 Sample Data

On first startup, the app seeds demo data:

| Tenant | ID | Plan |
|--------|----|------|
| Acme Corp | (auto-generated) | ENTERPRISE |
| Startup Inc | (auto-generated) | STARTER |
| FreeUser LLC | (auto-generated) | FREE |

**Test Credentials (Acme Corp):**
- `admin@acme.com` / `admin123` (SUPER_ADMIN)
- `manager@acme.com` / `manager123` (TENANT_ADMIN)
- `user@acme.com` / `user123` (USER)

## 🔷 Testing

```bash
# Run unit tests
mvn test

# Run specific test class
mvn test -Dtest=TenantServiceTest

# Run integration tests (requires MongoDB)
mvn test -Dtest=TenantIsolationIntegrationTest
```

## 🔷 Swagger UI

Access API documentation at: `http://localhost:8080/swagger-ui.html`

## 🔷 Billing Formula

```
cost = apiCalls × $0.01 + storageUsed × $0.001
```

Monthly invoices are auto-generated on the 1st of each month via Spring Scheduler.

## 🔷 Product Inventory & Sell Flow

Each product has a `productCount` field representing available stock.

### How it works
1. **Admin creates product** with `productCount` (e.g., 50 units)
2. **User (employee) sells** via `POST /products/{id}/sell?quantity=N`
3. `productCount` decreases by `N` on each sale
4. When `productCount` reaches **0** → product `active` is automatically set to **false**
5. Inactive products **cannot be sold** — returns `400 Bad Request`

### Validation Rules
| Scenario | Response |
|----------|----------|
| Sell normally | `200` — updated product with new count |
| Sell more than available stock | `400` — "Insufficient stock" |
| Sell inactive product | `400` — "Product is not active" |
| Product count reaches 0 | Auto-deactivated, `active: false` |

### Example
```bash
# Admin creates product with 50 units
POST /products  { "name": "Widget", "price": 29.99, "productCount": 50 }

# Employee sells 3 units
POST /products/{id}/sell?quantity=3
# Response: { "productCount": 47, "active": true }

# Employee sells remaining 47 units
POST /products/{id}/sell?quantity=47
# Response: { "productCount": 0, "active": false }

# Employee tries to sell again — blocked
POST /products/{id}/sell?quantity=1
# Response: 400 "Product is not active and cannot be sold"
```

## 🔷 Rate Limiting

| Plan | Requests/Minute |
|------|----------------|
| FREE | 20 |
| STARTER | 60 |
| ENTERPRISE | 200 |

## 🔷 Project Structure

```
src/main/java/com/multitenant/saas/
├── MultiTenantSaasApplication.java
├── aspect/          # Audit logging AOP
├── config/          # TenantContext, WebMvc, DataLoader, OpenAPI
├── controller/      # REST controllers
├── dto/             # Data transfer objects
├── exception/       # Custom exceptions & GlobalExceptionHandler
├── interceptor/     # Tenant & RateLimit interceptors
├── model/           # MongoDB entities
├── repository/      # Spring Data MongoDB repositories
├── security/        # JWT, SecurityConfig, UserDetailsService
└── service/         # Business logic
```

