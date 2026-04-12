# Lambda Invoice Generator

AWS Lambda function for the **Multi-Tenant SaaS Platform** that:

1. **Calculates billing** based on subscription plan (FREE / STARTER / ENTERPRISE)
2. **Generates a PDF invoice** with usage breakdown, rates, discounts, and totals
3. **Uploads the PDF to S3** at `tenants/{tenantId}/invoices/{month}/invoice.pdf`
4. **Returns the S3 key and amount** to the Spring Boot invoker

## Billing Rates

| Plan | Cost per API Call | Cost per Storage (MB) | Volume Discount |
|------|------------------|-----------------------|----------------|
| FREE | $0.0200 | $0.0020 | 0% |
| STARTER | $0.0100 | $0.0010 | 0% |
| ENTERPRISE | $0.0050 | $0.0005 | 10% |

## Build

```bash
cd lambda-invoice
mvn clean package
```

This produces a fat JAR at `target/lambda-invoice-1.0.0.jar` (~15 MB) with all dependencies included.

## Deploy to AWS Lambda

### Via AWS Console

1. Go to **AWS Lambda → Create function**
2. **Function name:** `generate-tenant-invoice`
3. **Runtime:** Java 17
4. **Architecture:** x86_64
5. **Handler:** `com.multitenant.lambda.InvoiceLambdaHandler::handleRequest`
6. Upload `target/lambda-invoice-1.0.0.jar` as the deployment package
7. Set **Memory:** 512 MB, **Timeout:** 30 seconds
8. Add **Environment variable:**
   - `S3_BUCKET` = `multi-tenant-saas-common-assets` (your S3 bucket name)

### Via AWS CLI

```bash
# Create the function
aws lambda create-function \
  --function-name generate-tenant-invoice \
  --runtime java17 \
  --handler com.multitenant.lambda.InvoiceLambdaHandler::handleRequest \
  --role arn:aws:iam::YOUR_ACCOUNT_ID:role/lambda-invoice-role \
  --zip-file fileb://target/lambda-invoice-1.0.0.jar \
  --memory-size 512 \
  --timeout 30 \
  --environment Variables={S3_BUCKET=multi-tenant-saas-common-assets}

# Update existing function
aws lambda update-function-code \
  --function-name generate-tenant-invoice \
  --zip-file fileb://target/lambda-invoice-1.0.0.jar
```

## Required IAM Permissions

The Lambda execution role needs:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::multi-tenant-saas-common-assets/tenants/*/invoices/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

## Input / Output

### Input (from Spring Boot app)

```json
{
  "action": "GENERATE_PDF",
  "tenantId": "69d2ad86b69e1f3b75f2fd72",
  "subscriptionPlan": "ENTERPRISE",
  "month": "2026-04",
  "apiCalls": 1000,
  "storageUsed": 500.0
}
```

### Output (returned to Spring Boot app)

```json
{
  "amount": 4.73,
  "pdfS3Key": "tenants/69d2ad86b69e1f3b75f2fd72/invoices/2026-04/invoice.pdf",
  "status": "GENERATED",
  "pdfSizeBytes": 2847
}
```

### Error Output

```json
{
  "error": "tenantId is required",
  "status": "FAILED"
}
```

## Flow in the SaaS Platform

```
Spring Boot App                         AWS Lambda                          S3
     │                                       │                               │
     │  POST /billing/generate/2026-04       │                               │
     │  ──────────────────────────────►      │                               │
     │  (synchronous invoke via SDK)         │                               │
     │                                       │                               │
     │                                  1. Calculate billing                 │
     │                                  2. Generate PDF (iText)              │
     │                                       │                               │
     │                                       │  PutObject (invoice.pdf)      │
     │                                       │  ─────────────────────────►   │
     │                                       │  ◄─────────────────────────   │
     │                                       │        200 OK                 │
     │                                       │                               │
     │  ◄──────────────────────────────      │                               │
     │  { amount, pdfS3Key, status }         │                               │
     │                                       │                               │
     │  Store pdfS3Key in Invoice (MongoDB)  │                               │
     │                                       │                               │
     │  GET /billing/invoice/2026-04         │                               │
     │  → response includes pdfDownloadUrl   │                               │
     │    (pre-signed S3 URL, 15-min expiry) │                               │
```

## Test Locally

You can test the billing calculation and PDF generation locally (without S3):

```bash
# Build
mvn clean package

# Test with a sample event
echo '{"tenantId":"test-123","subscriptionPlan":"ENTERPRISE","month":"2026-04","apiCalls":1000,"storageUsed":500.0}' > test-event.json
```

## PDF Contents

The generated PDF includes:
- **Header:** "INVOICE" with invoice period and tenant details
- **Invoice details:** Tenant ID, subscription plan, generation date
- **Usage breakdown table:** API calls and storage with per-unit rates
- **Totals:** Subtotal, discount (if ENTERPRISE), and final total
- **Footer:** Payment terms and contact info

## Project Structure

```
lambda-invoice/
├── pom.xml                              # Maven build with shade plugin (fat JAR)
├── README.md                            # This file
└── src/main/java/com/multitenant/lambda/
    └── InvoiceLambdaHandler.java        # Lambda handler (billing + PDF + S3)
```

