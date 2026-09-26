package org.ngolibrary.service;

import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.BookIssue;
import org.ngolibrary.domain.IssueStatus;
import org.ngolibrary.repo.BookIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverdueNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OverdueNotificationService.class);

    @Value("${app.whatsapp.notification-interval-days:3}")
    private int notificationIntervalDays;

    private final BookIssueRepository issueRepository;
    private final WhatsAppService whatsAppService;

    public OverdueNotificationService(BookIssueRepository issueRepository, WhatsAppService whatsAppService) {
        this.issueRepository = issueRepository;
        this.whatsAppService = whatsAppService;
    }

    @Scheduled(cron = "${app.whatsapp.cron:0 0 9 * * *}")
    @Transactional
    public void checkAndNotifyOverdue() {
        log.info("Running overdue book check...");

        LocalDate today = LocalDate.now();
        List<BookIssue> overdueIssues = issueRepository.findByStatusAndDueDateBefore(IssueStatus.ISSUED, today);

        log.info("Found {} overdue issues", overdueIssues.size());

        int sentCount = 0;
        for (BookIssue issue : overdueIssues) {
            if (shouldSendNotification(issue, today)) {
                String phone = issue.getStudent().getPhone();
                boolean hasContact = false;

                if (phone != null && !phone.trim().isEmpty()) {
                    whatsAppService.sendOverdueNotification(
                        phone,
                        issue.getStudent().getFullName(),
                        issue.getBook().getTitle(),
                        issue.getDueDate().toString()
                    );
                    hasContact = true;
                }

                if (hasContact) {
                    issue.setLastOverdueNotificationDate(today);
                    issueRepository.save(issue);
                    sentCount++;
                    log.info("Sent overdue notification for issue {} (phone: yes)", issue.getId());
                } else {
                    log.warn("Student {} has no phone for overdue notification", issue.getStudent().getUsername());
                }
            } else {
                log.debug("Skipping notification for issue {} - last sent on {}, next due on {}", 
                    issue.getId(), 
                    issue.getLastOverdueNotificationDate(),
                    issue.getLastOverdueNotificationDate() != null ? issue.getLastOverdueNotificationDate().plusDays(notificationIntervalDays) : "never");
            }
        }

        log.info("Sent {} overdue notifications", sentCount);
        checkAndNotifyDueSoon();
    }

    private boolean shouldSendNotification(BookIssue issue, LocalDate today) {
        LocalDate lastSent = issue.getLastOverdueNotificationDate();
        if (lastSent == null) {
            return true; // First notification
        }
        return !lastSent.plusDays(notificationIntervalDays).isAfter(today);
    }

    @Transactional(readOnly = true)
    public void checkAndNotifyDueSoon() {
        LocalDate today = LocalDate.now();
        LocalDate in2Days = today.plusDays(2);

        List<BookIssue> dueSoonIssues = issueRepository.findByStatusAndDueDateBetween(IssueStatus.ISSUED, today, in2Days);

        log.info("Found {} issues due in 1-2 days", dueSoonIssues.size());

        for (BookIssue issue : dueSoonIssues) {
            String phone = issue.getStudent().getPhone();
            boolean hasContact = false;

            if (phone != null && !phone.trim().isEmpty()) {
                int daysLeft = (int) java.time.temporal.ChronoUnit.DAYS.between(today, issue.getDueDate());
                if (daysLeft >= 0 && daysLeft <= 2) {
                    whatsAppService.sendDueSoonNotification(
                        phone,
                        issue.getStudent().getFullName(),
                        issue.getBook().getTitle(),
                        issue.getDueDate().toString(),
                        daysLeft
                    );
                    hasContact = true;
                }
            }

            if (!hasContact) {
                log.debug("Student {} has no phone for due-soon notification", issue.getStudent().getUsername());
            }
        }
    }

    public void notifyForIssue(BookIssue issue) {
        String phone = issue.getStudent().getPhone();
        boolean hasContact = false;

        if (phone != null && !phone.trim().isEmpty()) {
            whatsAppService.sendOverdueNotification(
                phone,
                issue.getStudent().getFullName(),
                issue.getBook().getTitle(),
                issue.getDueDate().toString()
            );
            hasContact = true;
        }

        if (hasContact) {
            issue.setLastOverdueNotificationDate(LocalDate.now());
            issueRepository.save(issue);
        }
    }
}