package org.ngolibrary.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.ngolibrary.domain.Book;
import org.ngolibrary.dto.BookRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookImportServiceImpl implements BookImportService {

    private static final Logger log = LoggerFactory.getLogger(BookImportServiceImpl.class);
    private final BookService bookService;

    public BookImportServiceImpl(BookService bookService) {
        this.bookService = bookService;
    }

    @Override
    @Transactional
    public BulkImportResult importFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int successful = 0;
        int failed = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                totalRows++;

                try {
                    BookRequest request = parseRow(row);
                    bookService.create(request);
                    successful++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    log.warn("Failed to import row {}: {}", i + 1, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error reading Excel file", e);
            errors.add("File processing error: " + e.getMessage());
        }

        return new BulkImportResult(totalRows, successful, failed, errors);
    }

    private boolean isEmptyRow(Row row) {
        for (int c = 0; c < 3; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && 
                !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private BookRequest parseRow(Row row) {
        BookRequest request = new BookRequest();
        
        // Required fields
        request.setTitle(getStringCellValue(row, 0));
        request.setAuthor(getStringCellValue(row, 1));
        request.setCategory(getStringCellValue(row, 2));
        request.setTotalCopies(getIntCellValue(row, 3));
        
        // Optional fields
        request.setDescription(getStringCellValue(row, 4));
        request.setDate(parseDate(row, 5));
        request.setRegistrationNumber(getStringCellValue(row, 6));
        request.setPublication(getStringCellValue(row, 7));
        request.setPublicationYear(getIntCellValue(row, 8));
        request.setPageCount(getIntCellValue(row, 9));
        request.setSeller(getStringCellValue(row, 10));
        request.setReceiptNumber(getStringCellValue(row, 11));
        request.setPrice(getDoubleCellValue(row, 12));

        validateRequest(request);
        return request;
    }

    private void validateRequest(BookRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (request.getAuthor() == null || request.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author is required");
        }
        if (request.getTotalCopies() == null || request.getTotalCopies() < 1) {
            throw new IllegalArgumentException("Total copies must be at least 1");
        }
    }

    private String getStringCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Integer getIntCellValue(Row row, int cellIndex) {
        String val = getStringCellValue(row, cellIndex);
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDoubleCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private LocalDate parseDate(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate();
                    }
                    break;
                case STRING:
                    String val = cell.getStringCellValue().trim();
                    if (!val.isEmpty()) {
                        // Try multiple date formats
                        for (String pattern : new String[]{"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd"}) {
                            try {
                                return LocalDate.parse(val, DateTimeFormatter.ofPattern(pattern));
                            } catch (Exception ignored) {}
                        }
                    }
                    break;
            }
        } catch (Exception ignored) {}
        return null;
    }
}