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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Imports books from the first sheet of an Excel file. The first row holds the headings and each column is
 * found by its heading, in any order; columns the import doesn't know (Total Copies, Description …) are skipped.
 */
@Service
public class BookImportServiceImpl implements BookImportService {

    private static final Logger log = LoggerFactory.getLogger(BookImportServiceImpl.class);

    /** The headings each field accepts, compared on letters and digits only ("Reg. No." = "regno"). */
    static final Map<String, List<String>> HEADINGS = Map.ofEntries(
            Map.entry("title", List.of("title", "booktitle")),
            Map.entry("author", List.of("author")),
            Map.entry("category", List.of("categoryno", "categorynumber", "category")),
            Map.entry("date", List.of("date", "dateyyyymmdd")),
            Map.entry("registration", List.of("registrationnumber", "registrationno", "regno", "regnumber")),
            Map.entry("publication", List.of("publication", "publisher")),
            Map.entry("year", List.of("publicationyear")),
            Map.entry("pages", List.of("pagecount", "pages")),
            Map.entry("seller", List.of("seller")),
            Map.entry("receipt", List.of("receiptnumber", "receiptno")),
            Map.entry("price", List.of("price")));
    private final BookService bookService;
    private final BookCategoryService categoryService;

    public BookImportServiceImpl(BookService bookService, BookCategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }

    @Override
    @Transactional
    public BulkImportResult importFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int totalRows = 0;
        int successful = 0;
        int failed = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columns = columns(sheet.getRow(0));
            if (!columns.containsKey("title") || !columns.containsKey("author")) {
                errors.add("The first row must have the headings Title and Author.");
                return new BulkImportResult(0, 0, 0, errors, warnings);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, columns)) {
                    continue;
                }
                totalRows++;

                try {
                    BookRequest request = parseRow(row, columns, i + 1, warnings);
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

        return new BulkImportResult(totalRows, successful, failed, errors, warnings);
    }

    /** Field → column number, from the heading row; a field with no matching heading is left out. */
    static Map<String, Integer> columns(Row headings) {
        Map<String, Integer> columns = new HashMap<>();
        if (headings == null) {
            return columns;
        }
        for (Cell cell : headings) {
            String heading = new DataFormatter().formatCellValue(cell).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
            HEADINGS.forEach((field, names) -> {
                if (names.contains(heading)) {
                    columns.putIfAbsent(field, cell.getColumnIndex());
                }
            });
        }
        return columns;
    }

    /** A row with no title and no author is a blank line. */
    private boolean isEmptyRow(Row row, Map<String, Integer> columns) {
        return getStringCellValue(row, columns.get("title")) == null && getStringCellValue(row, columns.get("author")) == null;
    }

    private BookRequest parseRow(Row row, Map<String, Integer> columns, int rowNumber, List<String> warnings) {
        BookRequest request = new BookRequest();

        // Required fields
        request.setTitle(getStringCellValue(row, columns.get("title")));
        request.setAuthor(getStringCellValue(row, columns.get("author")));
        // The category number from the master; an unknown number imports the book without one.
        String categoryNumber = getCategoryNumber(row, columns.get("category"));
        if (categoryNumber != null) {
            categoryService.findByNumber(categoryNumber).ifPresentOrElse(
                    category -> request.setCategoryId(category.getId()),
                    () -> warnings.add("Row " + rowNumber + ": category number " + categoryNumber
                            + " is not in the category master, so the book was added without a category"));
        }

        // Optional fields
        request.setDate(parseDate(row, columns.get("date")));
        request.setRegistrationNumber(getStringCellValue(row, columns.get("registration")));
        request.setPublication(getStringCellValue(row, columns.get("publication")));
        request.setPublicationYear(getIntCellValue(row, columns.get("year")));
        request.setPageCount(getIntCellValue(row, columns.get("pages")));
        request.setSeller(getStringCellValue(row, columns.get("seller")));
        request.setReceiptNumber(getStringCellValue(row, columns.get("receipt")));
        request.setPrice(getDoubleCellValue(row, columns.get("price")));

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
    }

    private String getStringCellValue(Row row, Integer cellIndex) {
        Cell cell = cellIndex == null ? null : row.getCell(cellIndex);
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

    /** Category number exactly as the cell shows it in Excel (e.g. "456", "456.0", "456.83"); null if blank. */
    private String getCategoryNumber(Row row, Integer cellIndex) {
        Cell cell = cellIndex == null ? null : row.getCell(cellIndex);
        if (cell == null) return null;
        FormulaEvaluator evaluator = row.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        String shown = new DataFormatter().formatCellValue(cell, evaluator).trim();
        return shown.isEmpty() ? null : shown;
    }

    private Integer getIntCellValue(Row row, Integer cellIndex) {
        String val = getStringCellValue(row, cellIndex);
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDoubleCellValue(Row row, Integer cellIndex) {
        Cell cell = cellIndex == null ? null : row.getCell(cellIndex);
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

    private LocalDate parseDate(Row row, Integer cellIndex) {
        Cell cell = cellIndex == null ? null : row.getCell(cellIndex);
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