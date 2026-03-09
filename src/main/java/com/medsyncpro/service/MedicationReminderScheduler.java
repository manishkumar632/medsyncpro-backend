package com.medsyncpro.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationReminderScheduler {

    private final MedicationWorkflowService medicationWorkflowService;

    @Scheduled(cron = "0 * * * * *")
    public void runReminderCycle() {
        try {
            medicationWorkflowService.processDueReminders();
            medicationWorkflowService.reconcileMissedDoses();
        } catch (Exception e) {
            log.error("Medication reminder cycle failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void runAdherenceAlertCycle() {
        try {
            medicationWorkflowService.processAdherenceAlerts();
        } catch (Exception e) {
            log.error("Medication adherence alert cycle failed: {}", e.getMessage(), e);
        }
    }
}
