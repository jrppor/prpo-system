package com.jirapat.prpo.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jirapat.prpo.dto.response.PurchaseRequestItemResponse;
import com.jirapat.prpo.dto.response.PurchaseRequestResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequestPdfExportService {

    private static final String TEMPLATE_PATH = "/reports/purchase_request.jrxml";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    @Value("${app.company.name}")
    private String companyName;

    private final PurchaseRequestService purchaseRequestService;

    public byte[] exportToPdf(UUID purchaseRequestId) {
        PurchaseRequestResponse pr = purchaseRequestService.getPurchaseRequestById(purchaseRequestId);
        log.info("Exporting purchase request {} to PDF", pr.getPrNumber());

        try (InputStream templateStream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (templateStream == null) {
                throw new IllegalStateException("Report template not found: " + TEMPLATE_PATH);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            Map<String, Object> parameters = buildParameters(pr);

            List<Map<String, Object>> itemData = pr.getItems().stream()
                    .map(this::mapItem)
                    .toList();

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemData);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, out);
            return out.toByteArray();

        } catch (JRException e) {
            log.error("Error generating PDF for purchase request {}: {}", purchaseRequestId, e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        } catch (Exception e) {
            log.error("Error generating PDF for purchase request {}: {}", purchaseRequestId, e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private Map<String, Object> buildParameters(PurchaseRequestResponse pr) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", companyName);
        params.put("prNumber", pr.getPrNumber());
        params.put("title", pr.getTitle());
        params.put("status", pr.getStatus() != null ? pr.getStatus().name() : "");
        params.put("requester", pr.getRequester() != null ? pr.getRequester() : "");
        params.put("department", pr.getDepartment());
        params.put("justification", pr.getJustification());
        params.put("requiredDate", pr.getRequiredDate() != null ? pr.getRequiredDate().format(DATE_FORMATTER) : null);
        params.put("totalAmount", pr.getTotalAmount() != null ? DECIMAL_FORMAT.format(pr.getTotalAmount()) : "0.00");
        params.put("createdAt", pr.getCreatedAt() != null ? pr.getCreatedAt().format(DATETIME_FORMATTER) : null);
        return params;
    }

    private Map<String, Object> mapItem(PurchaseRequestItemResponse item) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemNumber", item.getItemNumber());
        map.put("description", item.getDescription());
        map.put("quantity", item.getQuantity() != null ? DECIMAL_FORMAT.format(item.getQuantity()) : "");
        map.put("unit", item.getUnit());
        map.put("estimatedPrice", item.getEstimatedPrice() != null ? DECIMAL_FORMAT.format(item.getEstimatedPrice()) : "");
        map.put("totalPrice", item.getTotalPrice() != null ? DECIMAL_FORMAT.format(item.getTotalPrice()) : "");
        return map;
    }
}
