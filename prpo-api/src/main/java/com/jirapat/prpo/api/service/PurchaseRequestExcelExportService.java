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

import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;
import com.jirapat.prpo.api.repository.specification.PurchaseRequestSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequestExcelExportService {

    private static final String SHEET_NAME = "Purchase Requests";
    private static final String[] HEADERS = {
            "PR Number", "Title", "Status", "Total Amount",
            "Department", "Requester", "Required Date", "Created At"
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PurchaseRequestRepository purchaseRequestRepository;

    @Transactional(readOnly = true)
    public byte[] exportToExcel(PurchaseRequestStatus status, String department,
                                LocalDate dateFrom, LocalDate dateTo, String search) throws IOException {

        Specification<PurchaseRequest> spec = Specification
                .where(PurchaseRequestSpecification.hasStatus(status))
                .and(PurchaseRequestSpecification.hasDepartment(department))
                .and(PurchaseRequestSpecification.createdAfter(dateFrom))
                .and(PurchaseRequestSpecification.createdBefore(dateTo))
                .and(PurchaseRequestSpecification.searchByKeyword(search));

        List<PurchaseRequest> purchaseRequests = purchaseRequestRepository
                .findAll(spec, Pageable.unpaged())
                .getContent();

        log.info("Exporting {} purchase requests to Excel", purchaseRequests.size());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = createHeaderStyle(workbook);

            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, purchaseRequests);
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

    private void fillDataRows(Sheet sheet, List<PurchaseRequest> purchaseRequests) {
        int rowNum = 1;
        for (PurchaseRequest pr : purchaseRequests) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(pr.getPrNumber());
            row.createCell(1).setCellValue(pr.getTitle());
            row.createCell(2).setCellValue(pr.getStatus() != null ? pr.getStatus().name() : "");
            row.createCell(3).setCellValue(pr.getTotalAmount() != null ? pr.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(pr.getDepartment() != null ? pr.getDepartment() : "");
            row.createCell(5).setCellValue(formatRequester(pr.getRequester()));
            row.createCell(6).setCellValue(pr.getRequiredDate() != null ? pr.getRequiredDate().format(DATE_FORMATTER) : "");
            row.createCell(7).setCellValue(pr.getCreatedAt() != null ? pr.getCreatedAt().format(DATETIME_FORMATTER) : "");
        }
    }

    private String formatRequester(User user) {
        if (user == null) return "";
        return user.getFirstName() + " " + user.getLastName();
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
