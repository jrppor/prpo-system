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

import com.jirapat.prpo.dto.response.PurchaseOrderItemResponse;
import com.jirapat.prpo.dto.response.PurchaseOrderResponse;

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
public class PurchaseOrderPdfExportService {

    private static final String TEMPLATE_PATH = "/reports/purchase_order.jrxml";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    @Value("${app.company.name}")
    private String companyName;

    private final PurchaseOrderService purchaseOrderService;

    public byte[] exportToPdf(UUID purchaseOrderId) {
        PurchaseOrderResponse po = purchaseOrderService.getPurchaseOrderById(purchaseOrderId);
        log.info("Exporting purchase order {} to PDF", po.getPoNumber());

        try (InputStream templateStream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (templateStream == null) {
                throw new IllegalStateException("Report template not found: " + TEMPLATE_PATH);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            Map<String, Object> parameters = buildParameters(po);

            List<Map<String, Object>> itemData = po.getItems().stream()
                    .map(this::mapItem)
                    .toList();

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemData);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, out);
            return out.toByteArray();

        } catch (JRException e) {
            log.error("Error generating PDF for purchase order {}: {}", purchaseOrderId, e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        } catch (Exception e) {
            log.error("Error generating PDF for purchase order {}: {}", purchaseOrderId, e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private Map<String, Object> buildParameters(PurchaseOrderResponse po) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", companyName);
        params.put("poNumber", po.getPoNumber());
        params.put("purchaseRequestNumber", po.getPurchaseRequestNumber());
        params.put("vendorName", po.getVendorName());
        params.put("createdByName", po.getCreatedByName());
        params.put("status", po.getStatus() != null ? po.getStatus().name() : "");
        params.put("orderDate", po.getOrderDate() != null ? po.getOrderDate().format(DATE_FORMATTER) : null);
        params.put("expectedDeliveryDate", po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate().format(DATE_FORMATTER) : null);
        params.put("remark", po.getRemark());
        params.put("totalAmount", po.getTotalAmount() != null ? DECIMAL_FORMAT.format(po.getTotalAmount()) : "0.00");
        params.put("createdAt", po.getCreatedAt() != null ? po.getCreatedAt().format(DATETIME_FORMATTER) : null);
        return params;
    }

    private Map<String, Object> mapItem(PurchaseOrderItemResponse item) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemNumber", item.getItemNumber());
        map.put("description", item.getDescription());
        map.put("quantity", item.getQuantity() != null ? DECIMAL_FORMAT.format(item.getQuantity()) : "");
        map.put("unit", item.getUnit());
        map.put("unitPrice", item.getUnitPrice() != null ? DECIMAL_FORMAT.format(item.getUnitPrice()) : "");
        map.put("totalPrice", item.getTotalPrice() != null ? DECIMAL_FORMAT.format(item.getTotalPrice()) : "");
        return map;
    }
}
