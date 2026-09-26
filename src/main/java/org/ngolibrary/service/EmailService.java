package org.ngolibrary.service;

public interface EmailService {
    void sendOverdueNotification(String toEmail, String studentName, String bookTitle, String dueDate);
    void sendDueSoonNotification(String toEmail, String studentName, String bookTitle, String dueDate, int daysLeft);
}