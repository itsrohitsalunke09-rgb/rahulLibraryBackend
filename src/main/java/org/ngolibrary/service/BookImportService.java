package org.ngolibrary.service;

import org.ngolibrary.domain.Book;
import org.ngolibrary.dto.BookRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookImportService {
    BulkImportResult importFromExcel(MultipartFile file);
    
    class BulkImportResult {
        private int totalRows;
        private int successful;
        private int failed;
        private List<String> errors;
        
        public BulkImportResult(int totalRows, int successful, int failed, List<String> errors) {
            this.totalRows = totalRows;
            this.successful = successful;
            this.failed = failed;
            this.errors = errors;
        }
        
        public int getTotalRows() { return totalRows; }
        public int getSuccessful() { return successful; }
        public int getFailed() { return failed; }
        public List<String> getErrors() { return errors; }
    }
}