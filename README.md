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
| AWS S3 | Tenant-isolated product image & file storage |
| AWS Lambda | invoice generation |
| AWS CloudWatch | Per-tenant API call & event metrics |
| AWS Secrets Manager | Secure credential management (JWT secret, DB URI) |

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

### Automatic Usage Metering
- **API calls** are tracked automatically via `UsageTrackingFilter` (servlet filter)
- Every successful authenticated request increments the tenant's monthly `apiCalls` counter
- **Storage** is tracked automatically on S3 file uploads (bytes → MB)
- Tenants can only **view** their usage (`GET /usage`), not self-report it
- `SUPER_ADMIN` can manually adjust usage via `POST /usage/admin/adjust` for corrections

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

### Usage (Auto-Tracked)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/usage` | Get usage data (auto-tracked by system) | SUPER_ADMIN, TENANT_ADMIN |
| POST | `/usage/admin/adjust` | Manually adjust usage (corrections only) | SUPER_ADMIN only |

> **Note:** API calls are **automatically tracked** by the `UsageTrackingFilter` — every successful authenticated request increments the tenant's `apiCalls` counter. Storage usage is tracked automatically on S3 file uploads. Tenants **cannot self-report** usage; they can only **view** it.

### Billing
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/billing/invoice` | Get all invoices (includes pre-signed PDF URL if available) | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/billing/invoice/{month}` | Get invoice for month (includes pre-signed PDF URL) | SUPER_ADMIN, TENANT_ADMIN |
| POST | `/billing/generate/{month}` | Generate invoice (Lambda generates PDF → S3 → returns key) | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/billing/invoice/{month}/download` | Get a fresh pre-signed PDF download URL (15-min expiry) | SUPER_ADMIN, TENANT_ADMIN |

### S3 Storage (AWS profile only)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/storage/products/{productId}/image` | Upload product image to S3 | SUPER_ADMIN, TENANT_ADMIN |
| POST | `/storage/files` | Upload file to tenant S3 storage | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/storage/files` | List all tenant files in S3 | SUPER_ADMIN, TENANT_ADMIN |
| DELETE | `/storage/products/{productId}/assets` | Delete all S3 assets for a product | SUPER_ADMIN, TENANT_ADMIN |
| GET | `/storage/products/{productId}/image-url` | Get product image URL | Any authenticated |

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

- `apiCalls` — automatically incremented by `UsageTrackingFilter` on every successful request
- `storageUsed` — automatically incremented on S3 file/image uploads (tracked in MB)
- Monthly invoices are auto-generated on the 1st of each month via Spring Scheduler.

### Invoice PDF Flow (AWS profile)

When running with the `aws` profile, invoices follow a Lambda → S3 → Pre-signed URL flow:

```
POST /billing/generate/{month}
         │
         ▼
┌─────────────────────────┐
│  BillingService         │
│  generateInvoice()      │
└──────────┬──────────────┘
           │ synchronous call
           ▼
┌─────────────────────────┐
│  AWS Lambda             │
│  generate-tenant-invoice│
│  1. Calculate billing   │
│  2. Generate PDF        │
│  3. Upload PDF to S3    │
│  4. Return {amount,     │
│     pdfS3Key}           │
└──────────┬──────────────┘
           │ returns S3 key
           ▼
┌─────────────────────────┐
│  BillingService         │
│  Store pdfS3Key in      │
│  Invoice (MongoDB)      │
└──────────┬──────────────┘
           │
           ▼
  GET /billing/invoice/{month}
  → Response includes pdfDownloadUrl
    (pre-signed S3 URL, 15-min expiry)

  GET /billing/invoice/{month}/download
  → Fresh pre-signed URL for PDF download
```

**Without AWS profile:** Billing is calculated locally, no PDF is generated, `pdfS3Key` and `pdfDownloadUrl` are `null`.

## 🔷 Product Inventory & Sell Flow

Each product has a `productCount` field representing available stock. Sell operations use **MongoDB atomic `$inc` updates** (`MongoTemplate.updateFirst()`) to prevent race conditions — no external queues needed.

### How it works
1. **Admin creates product** with `productCount` (e.g., 50 units)
2. **User (employee) sells** via `POST /products/{id}/sell?quantity=N`
3. `productCount` decreases **atomically** by `N` — guaranteed safe under concurrency
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

## 🔷 AWS Integration

AWS services are **optional** and activate only when running with the `aws` profile. Without it, the platform runs fully on MongoDB + embedded/Docker MongoDB — no AWS account required.

### Enable AWS Profile
```bash
# Via environment variable
export SPRING_PROFILES_ACTIVE=aws

# Via command line
java -jar app.jar --spring.profiles.active=aws

# Via Docker
docker run -e SPRING_PROFILES_ACTIVE=aws -e AWS_ACCESS_KEY_ID=xxx -e AWS_SECRET_ACCESS_KEY=xxx saas-platform
```

### Required Environment Variables (AWS profile)
| Variable | Description | Example |
|----------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | AWS access key | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | `wJalrXUtnFEMI/K7MDENG/...` |
| `AWS_S3_BUCKET` | S3 bucket name | `multi-tenant-saas-common-assets` |
| `AWS_LAMBDA_INVOICE_FUNC` | Lambda function name | `generate-tenant-invoice` |

### AWS Services Summary

| Service | What It Does | Triggered By |
|---------|-------------|--------------|
| **S3** | Stores product images, tenant files, and **invoice PDFs** in isolated paths; generates **pre-signed download URLs** (15-min expiry) | `POST /storage/products/{id}/image`, `DELETE /products/{id}` (auto-cleanup), `POST /billing/generate/{month}` (PDF upload via Lambda) |
| **Lambda** | Calculates billing, **generates invoice PDF**, uploads PDF to S3, returns S3 key & amount | `POST /billing/generate/{month}` (synchronous), async trigger via `UsageTrackingFilter` |
| **CloudWatch** | Records per-tenant metrics: API calls, sell events, out-of-stock, usage | Every service call (via AuditAspect), sell events, usage updates |
| **Secrets Manager** | Securely stores JWT secret & MongoDB URI (replaces `application.yml` hardcoded values) | App startup |

### Databases
| Database | Type | Used For |
|----------|------|----------|
| **MongoDB** (Docker/Atlas) | NoSQL Document | Tenants, Products, Users, Usage, Invoices, Audit Logs |
| **S3** | Object Storage | Product images, tenant files |

### Architecture with AWS
```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot App                          │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │ Product  │  │  Usage   │  │ Billing  │  │   Audit   │  │
│  │ Service  │  │ Service  │  │ Service  │  │  Aspect   │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └─────┬─────┘  │
│       │              │              │               │       │
│  ┌────▼──────────────▼──────────────▼───────────────▼────┐  │
│  │           AWS Services (optional, @Profile("aws"))    │  │
│  │  ┌──────┐ ┌─────┐ ┌──────────────┐  │  │
│  │  │Lambda│ │ S3  │ │  CloudWatch  │  │  │
│  │  └──────┘ └─────┘ └──────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
│       │                                                     │
│  ┌────▼─────────────────────────────────────────────────┐   │
│  │              MongoDB (always active)                  │   │
│  │  tenants │ products │ users │ usages │ invoices │ ... │   │
│  └───────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 🔷 Project Structure

```
Multi-Tenant SaaS Platform/
├── src/main/java/com/multitenant/saas/
│   ├── MultiTenantSaasApplication.java
│   ├── aspect/          # Audit logging AOP + CloudWatch metrics
│   ├── config/          # TenantContext, WebMvc, DataLoader, OpenAPI, AwsConfig, SecretsManager
│   ├── controller/      # REST controllers + S3Controller (AWS)
│   ├── dto/             # Data transfer objects (InvoiceDTO includes pdfS3Key & pdfDownloadUrl)
│   ├── exception/       # Custom exceptions & GlobalExceptionHandler
│   ├── interceptor/     # Tenant, RateLimit & UsageTracking interceptors/filters
│   ├── model/           # MongoDB entities (Invoice includes pdfS3Key)
│   ├── repository/      # Spring Data MongoDB repositories
│   ├── security/        # JWT, SecurityConfig, UserDetailsService
│   └── service/         # Business logic + AWS services
│       ├── ProductService.java          # + atomic sells (MongoTemplate), CloudWatch, S3 integration
│       ├── UsageService.java            # Auto-tracked usage + CloudWatch, Lambda PDF invoice integration
│       ├── BillingService.java          # Invoice generation with Lambda PDF + S3 pre-signed URLs
│       ├── S3StorageService.java        # AWS S3 (tenant-isolated storage + pre-signed URL generation)
│       ├── LambdaInvoiceService.java    # AWS Lambda (invoice PDF generation + billing calculation)
│       └── CloudWatchMetricsService.java # AWS CloudWatch (per-tenant metrics)
│
└── lambda-invoice/                      # Deployable AWS Lambda function (separate Maven project)
    ├── pom.xml                          # Maven build with shade plugin → fat JAR
    ├── README.md                        # Lambda deployment & IAM instructions
    └── src/main/java/com/multitenant/lambda/
        └── InvoiceLambdaHandler.java    # Billing calc + PDF generation (iText) + S3 upload
```

