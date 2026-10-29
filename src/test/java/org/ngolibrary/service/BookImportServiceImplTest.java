package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class BookImportServiceImplTest {

    private static Row headings(String... names) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet().createRow(0);
            for (int i = 0; i < names.length; i++) {
                row.createCell(i).setCellValue(names[i]);
            }
            return row;
        }
    }

    @Test
    void findsColumnsByHeadingInAnyOrderAndSkipsUnknownOnes() throws Exception {
        Map<String, Integer> columns = BookImportServiceImpl.columns(
                headings("Reg. No.", "Author", "Total Copies", "Description", "TITLE", "Category No.", "Price"));
        assertEquals(0, columns.get("registration"));
        assertEquals(1, columns.get("author"));
        assertEquals(4, columns.get("title"));
        assertEquals(5, columns.get("category"));
        assertEquals(6, columns.get("price"));
        assertFalse(columns.containsKey("date"));
        assertEquals(5, columns.size());
    }

    @Test
    void readsTheOldThirteenColumnLayoutToo() throws Exception {
        Map<String, Integer> columns = BookImportServiceImpl.columns(headings("Title", "Author", "Category No.",
                "Total Copies", "Description", "Date (yyyy-MM-dd)", "Registration Number", "Publication",
                "Publication Year", "Page Count", "Seller", "Receipt Number", "Price"));
        assertEquals(5, columns.get("date"));
        assertEquals(6, columns.get("registration"));
        assertEquals(12, columns.get("price"));
        assertEquals(11, columns.size());
    }
}
