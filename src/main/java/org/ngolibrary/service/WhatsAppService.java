package org.ngolibrary.service;

public interface WhatsAppService {
    void sendOverdueNotification(String toPhone, String studentName, String bookTitle, String dueDate);
    void sendDueSoonNotification(String toPhone, String studentName, String bookTitle, String dueDate, int daysLeft);
}