package com.jirapat.prpo.api.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.api.entity.PurchaseOrder;
import com.jirapat.prpo.api.entity.PurchaseOrderStatus;
import com.jirapat.prpo.api.repository.PurchaseOrderRepository;
import com.jirapat.prpo.api.repository.specification.PurchaseOrderSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderExcelExportService {

    private static final String SHEET_NAME = "Purchase Orders";
    private static final String[] HEADERS = {
        "PO Number", "PR Number", "Status", "Total Amount",
        "Vendor Name", "Actual Delivery Date",
        "Expected Delivery Date", "Created At"
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(readOnly = true)
    public byte[] exportToExcel(PurchaseOrderStatus status,
            LocalDate dateFrom,
            LocalDate dateTo,
            String search) throws IOException {

        Specification<PurchaseOrder> spec = Specification
                .where(PurchaseOrderSpecification.hasStatus(status))
                .and(PurchaseOrderSpecification.createdAfter(dateFrom))
                .and(PurchaseOrderSpecification.createdBefore(dateTo))
                .and(PurchaseOrderSpecification.searchByKeyword(search));

        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository
                .findAll(spec, Pageable.unpaged())
                .getContent();

        log.info("Exporting {} purchase orders to Excel", purchaseOrders.size());

        try (Workbook workbook = new XSSFWorkbook(); 
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = createHeaderStyle(workbook);

            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, purchaseOrders);
            autoSizeColumns(sheet);

            workbook.write(out);
            return out.toByteArray();
        }

    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillDataRows(Sheet sheet, List<PurchaseOrder> purchaseOrders) {
        int rowNum = 1;
        for (PurchaseOrder po : purchaseOrders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(po.getPoNumber());
            row.createCell(1).setCellValue(po.getPurchaseRequest() != null ? po.getPurchaseRequest().getPrNumber() : "");
            row.createCell(2).setCellValue(po.getStatus() != null ? po.getStatus().name() : "");
            row.createCell(3).setCellValue(po.getTotalAmount() != null ? po.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(po.getVendor() != null ? po.getVendor().getName() : "");
            row.createCell(5).setCellValue(po.getActualDeliveryDate() != null ? po.getActualDeliveryDate().format(DATE_FORMATTER) : "");
            row.createCell(6).setCellValue(po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate().format(DATE_FORMATTER) : "");
            row.createCell(7).setCellValue(po.getCreatedAt() != null ? po.getCreatedAt().format(DATETIME_FORMATTER) : "");
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
