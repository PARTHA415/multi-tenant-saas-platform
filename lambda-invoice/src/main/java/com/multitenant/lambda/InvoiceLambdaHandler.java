package com.multitenant.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda function: Invoice PDF Generator
 *
 * Receives invoice data from the Multi-Tenant SaaS Platform,
 * calculates billing based on subscription plan,
 * generates a professional PDF invoice,
 * uploads it to S3, and returns the S3 key + amount.
 *
 * <h3>Input payload (from Spring Boot app):</h3>
 * <pre>
 * {
 *   "action": "GENERATE_PDF",
 *   "tenantId": "abc123",
 *   "subscriptionPlan": "ENTERPRISE",
 *   "month": "2026-04",
 *   "apiCalls": 1000,
 *   "storageUsed": 500.0
 * }
 * </pre>
 *
 * <h3>Output response:</h3>
 * <pre>
 * {
 *   "amount": 10.50,
 *   "pdfS3Key": "tenants/abc123/invoices/2026-04/invoice.pdf",
 *   "status": "GENERATED"
 * }
 * </pre>
 *
 * <h3>Environment variables (set in Lambda configuration):</h3>
 * <ul>
 *   <li><code>S3_BUCKET</code> — S3 bucket name (default: multi-tenant-saas-common-assets)</li>
 * </ul>
 *
 * <h3>Required IAM permissions:</h3>
 * <ul>
 *   <li><code>s3:PutObject</code> on the target bucket</li>
 *   <li>Basic Lambda execution role (CloudWatch Logs)</li>
 * </ul>
 *
 * <h3>Deploy:</h3>
 * <pre>
 * cd lambda-invoice
 * mvn clean package
 * # Upload target/lambda-invoice-1.0.0.jar to AWS Lambda
 * # Handler: com.multitenant.lambda.InvoiceLambdaHandler::handleRequest
 * # Runtime: Java 17
 * </pre>
 */
public class InvoiceLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String BUCKET_NAME = System.getenv("S3_BUCKET") != null
            ? System.getenv("S3_BUCKET") : "multi-tenant-saas-common-assets";

    // Billing rates per subscription plan
    private static final Map<String, double[]> PLAN_RATES = Map.of(
            // { costPerApiCall, costPerStorageUnit, discount% }
            "FREE",       new double[]{0.02,  0.002, 0.0},
            "STARTER",    new double[]{0.01,  0.001, 0.0},
            "ENTERPRISE", new double[]{0.005, 0.0005, 10.0}  // 10% volume discount
    );

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    public InvoiceLambdaHandler() {
        this.s3Client = S3Client.builder().build();  // uses Lambda execution role credentials
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing
    InvoiceLambdaHandler(S3Client s3Client) {
        this.s3Client = s3Client;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        context.getLogger().log("Received input: " + input);

        try {
            String tenantId = (String) input.get("tenantId");
            String subscriptionPlan = (String) input.getOrDefault("subscriptionPlan", "FREE");
            String month = (String) input.getOrDefault("month",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            long apiCalls = toLong(input.get("apiCalls"));
            double storageUsed = toDouble(input.get("storageUsed"));
            String action = (String) input.getOrDefault("action", "GENERATE_PDF");

            if (tenantId == null || tenantId.isBlank()) {
                return errorResponse("tenantId is required");
            }

            // 1. Calculate billing
            BillingResult billing = calculateBilling(subscriptionPlan, apiCalls, storageUsed);
            context.getLogger().log(String.format("Billing calculated: plan=%s, apiCost=%.2f, storageCost=%.2f, discount=%.2f, total=%.2f",
                    subscriptionPlan, billing.apiCost, billing.storageCost, billing.discount, billing.total));

            // 2. Generate PDF
            byte[] pdfBytes = generateInvoicePdf(tenantId, subscriptionPlan, month, apiCalls, storageUsed, billing);
            context.getLogger().log("PDF generated: " + pdfBytes.length + " bytes");

            // 3. Upload to S3
            String s3Key = String.format("tenants/%s/invoices/%s/invoice.pdf", tenantId, month);
            uploadToS3(s3Key, pdfBytes);
            context.getLogger().log("PDF uploaded to S3: " + s3Key);

            // 4. Return response
            Map<String, Object> response = new HashMap<>();
            response.put("amount", billing.total);
            response.put("pdfS3Key", s3Key);
            response.put("status", "GENERATED");
            response.put("pdfSizeBytes", pdfBytes.length);

            context.getLogger().log("Response: " + response);
            return response;

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    // ==================== BILLING CALCULATION ====================

    /**
     * Calculate billing based on subscription plan.
     * Each plan has different rates per API call and per storage unit (MB).
     * ENTERPRISE gets a 10% volume discount.
     */
    BillingResult calculateBilling(String plan, long apiCalls, double storageUsed) {
        double[] rates = PLAN_RATES.getOrDefault(plan.toUpperCase(), PLAN_RATES.get("FREE"));

        double costPerApiCall = rates[0];
        double costPerStorage = rates[1];
        double discountPercent = rates[2];

        double apiCost = apiCalls * costPerApiCall;
        double storageCost = storageUsed * costPerStorage;
        double subtotal = apiCost + storageCost;
        double discount = subtotal * (discountPercent / 100.0);
        double total = subtotal - discount;

        // Round to 2 decimal places
        total = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).doubleValue();

        return new BillingResult(apiCost, storageCost, subtotal, discount, total,
                costPerApiCall, costPerStorage, discountPercent);
    }

    // ==================== PDF GENERATION ====================

    /**
     * Generate a professional invoice PDF with iText.
     */
    byte[] generateInvoicePdf(String tenantId, String plan, String month,
                               long apiCalls, double storageUsed,
                               BillingResult billing) throws DocumentException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, baos);
        document.open();

        // ---- Fonts ----
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, new BaseColor(33, 37, 41));
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(33, 37, 41));
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(73, 80, 87));
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(108, 117, 125));
        Font amountFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(0, 123, 255));
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(134, 142, 150));

        // ---- Header ----
        Paragraph title = new Paragraph("INVOICE", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        document.add(new Paragraph(" "));

        // ---- Invoice Details Table ----
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{1, 1});

        // Left column: invoice info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("Invoice Period", labelFont));
        leftCell.addElement(new Paragraph(month, headerFont));
        leftCell.addElement(new Paragraph(" "));
        leftCell.addElement(new Paragraph("Tenant ID", labelFont));
        leftCell.addElement(new Paragraph(tenantId, normalFont));

        // Right column: plan & date
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph("Subscription Plan", labelFont));
        rightCell.addElement(new Paragraph(plan.toUpperCase(), headerFont));
        rightCell.addElement(new Paragraph(" "));
        rightCell.addElement(new Paragraph("Generated", labelFont));
        rightCell.addElement(new Paragraph(
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), normalFont));

        detailsTable.addCell(leftCell);
        detailsTable.addCell(rightCell);
        document.add(detailsTable);

        document.add(new Paragraph(" "));

        // ---- Separator Line ----
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell sepCell = new PdfPCell();
        sepCell.setBorder(Rectangle.BOTTOM);
        sepCell.setBorderColor(new BaseColor(222, 226, 230));
        sepCell.setBorderWidth(1);
        sepCell.setFixedHeight(5);
        separator.addCell(sepCell);
        document.add(separator);

        document.add(new Paragraph(" "));

        // ---- Usage Breakdown Table ----
        document.add(new Paragraph("Usage Breakdown", headerFont));
        document.add(new Paragraph(" "));

        PdfPTable usageTable = new PdfPTable(4);
        usageTable.setWidthPercentage(100);
        usageTable.setWidths(new float[]{3, 2, 2, 2});

        // Table header
        addTableHeader(usageTable, "Item", headerFont);
        addTableHeader(usageTable, "Quantity", headerFont);
        addTableHeader(usageTable, "Rate", headerFont);
        addTableHeader(usageTable, "Amount", headerFont);

        // Row 1: API Calls
        addTableCell(usageTable, "API Calls", normalFont);
        addTableCell(usageTable, String.format("%,d calls", apiCalls), normalFont);
        addTableCell(usageTable, String.format("$%.4f / call", billing.costPerApiCall), normalFont);
        addTableCell(usageTable, String.format("$%.2f", billing.apiCost), normalFont);

        // Row 2: Storage
        addTableCell(usageTable, "Storage Used", normalFont);
        addTableCell(usageTable, String.format("%.2f MB", storageUsed), normalFont);
        addTableCell(usageTable, String.format("$%.4f / MB", billing.costPerStorage), normalFont);
        addTableCell(usageTable, String.format("$%.2f", billing.storageCost), normalFont);

        document.add(usageTable);
        document.add(new Paragraph(" "));

        // ---- Totals ----
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[]{1, 1});

        addTotalRow(totalsTable, "Subtotal", String.format("$%.2f", billing.subtotal), normalFont);

        if (billing.discountPercent > 0) {
            addTotalRow(totalsTable, String.format("Discount (%.0f%%)", billing.discountPercent),
                    String.format("-$%.2f", billing.discount), normalFont);
        }

        // Total row with emphasis
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total", headerFont));
        totalLabel.setBorder(Rectangle.TOP);
        totalLabel.setBorderColor(new BaseColor(33, 37, 41));
        totalLabel.setBorderWidth(2);
        totalLabel.setPadding(8);
        totalsTable.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase(String.format("$%.2f", billing.total), amountFont));
        totalValue.setBorder(Rectangle.TOP);
        totalValue.setBorderColor(new BaseColor(33, 37, 41));
        totalValue.setBorderWidth(2);
        totalValue.setPadding(8);
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(totalValue);

        document.add(totalsTable);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // ---- Footer ----
        Paragraph footer = new Paragraph(
                "This invoice was auto-generated by the Multi-Tenant SaaS Platform.\n" +
                "Payment is due within 30 days of the invoice date.\n" +
                "For questions, contact billing@saas-platform.com",
                smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    // ==================== S3 UPLOAD ====================

    void uploadToS3(String s3Key, byte[] pdfBytes) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .contentType("application/pdf")
                .contentDisposition("inline; filename=invoice.pdf")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(pdfBytes));
    }

    // ==================== HELPERS ====================

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new BaseColor(248, 249, 250));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new BaseColor(222, 226, 230));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new BaseColor(245, 245, 245));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", "FAILED");
        return error;
    }

    private long toLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    // ==================== BILLING RESULT DTO ====================

    static class BillingResult {
        final double apiCost;
        final double storageCost;
        final double subtotal;
        final double discount;
        final double total;
        final double costPerApiCall;
        final double costPerStorage;
        final double discountPercent;

        BillingResult(double apiCost, double storageCost, double subtotal, double discount, double total,
                      double costPerApiCall, double costPerStorage, double discountPercent) {
            this.apiCost = apiCost;
            this.storageCost = storageCost;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.costPerApiCall = costPerApiCall;
            this.costPerStorage = costPerStorage;
            this.discountPercent = discountPercent;
        }
    }
}
