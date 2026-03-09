package com.medsyncpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsyncpro.dto.request.CreateMedicationScheduleRequest;
import com.medsyncpro.dto.request.DoseActionRequest;
import com.medsyncpro.dto.request.UpdateMedicationScheduleRequest;
import com.medsyncpro.dto.response.DoctorAdherenceAlertResponse;
import com.medsyncpro.dto.response.MedicationAdherenceSummaryResponse;
import com.medsyncpro.dto.response.MedicationDoseLogResponse;
import com.medsyncpro.dto.response.MedicationScheduleResponse;
import com.medsyncpro.entity.*;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationWorkflowService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationDoseLogRepository medicationDoseLogRepository;
    private final MedicationAdherenceAlertRepository medicationAdherenceAlertRepository;
    private final NotificationDispatchService notificationDispatchService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${medication.adherence.default-threshold:80.0}")
    private double defaultAdherenceThreshold;

    @Value("${medication.missed-dose-grace-minutes:120}")
    private int missedDoseGraceMinutes;

    @Transactional
    public MedicationScheduleResponse createSchedule(UUID patientUserId, CreateMedicationScheduleRequest request) {
        Patient patient = getPatientByUserId(patientUserId);

        validateScheduleRequest(request.getScheduleType(), request.getReminderTimes(), request.getReminderDays(),
                request.getStartDate(), request.getEndDate());

        Doctor doctor = null;
        if (request.getDoctorId() != null) {
            doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        }

        Prescription prescription = null;
        if (request.getPrescriptionId() != null) {
            prescription = prescriptionRepository.findById(request.getPrescriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
            if (!prescription.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Prescription does not belong to the patient");
            }
        }

        MedicationSchedule schedule = MedicationSchedule.builder()
                .patient(patient)
                .doctor(doctor)
                .prescription(prescription)
                .medicineName(request.getMedicineName().trim())
                .dosage(request.getDosage())
                .frequency(request.getFrequency())
                .instructions(request.getInstructions())
                .scheduleType(request.getScheduleType())
                .reminderTimes(toJsonList(normaliseTimes(request.getReminderTimes())))
                .reminderDays(toJsonList(normaliseDays(request.getReminderDays())))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reminderInApp(request.getReminderInApp() == null || request.getReminderInApp())
                .reminderEmail(Boolean.TRUE.equals(request.getReminderEmail()))
                .reminderPush(Boolean.TRUE.equals(request.getReminderPush()))
                .adherenceAlertThreshold(request.getAdherenceAlertThreshold() != null
                        ? request.getAdherenceAlertThreshold()
                        : defaultAdherenceThreshold)
                .active(true)
                .build();

        schedule = medicationScheduleRepository.save(schedule);
        return toScheduleResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<MedicationScheduleResponse> getSchedules(UUID patientUserId) {
        Patient patient = getPatientByUserId(patientUserId);
        return medicationScheduleRepository.findByPatientIdAndActiveTrueOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    @Transactional
    public MedicationScheduleResponse updateSchedule(
            UUID patientUserId,
            UUID scheduleId,
            UpdateMedicationScheduleRequest request) {
        Patient patient = getPatientByUserId(patientUserId);
        MedicationSchedule schedule = medicationScheduleRepository.findByIdAndPatientId(scheduleId, patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Medication schedule not found"));

        if (request.getMedicineName() != null && !request.getMedicineName().isBlank()) {
            schedule.setMedicineName(request.getMedicineName().trim());
        }
        if (request.getDosage() != null) {
            schedule.setDosage(request.getDosage());
        }
        if (request.getFrequency() != null) {
            schedule.setFrequency(request.getFrequency());
        }
        if (request.getInstructions() != null) {
            schedule.setInstructions(request.getInstructions());
        }
        if (request.getScheduleType() != null) {
            schedule.setScheduleType(request.getScheduleType());
        }
        if (request.getReminderTimes() != null) {
            schedule.setReminderTimes(toJsonList(normaliseTimes(request.getReminderTimes())));
        }
        if (request.getReminderDays() != null) {
            schedule.setReminderDays(toJsonList(normaliseDays(request.getReminderDays())));
        }
        if (request.getStartDate() != null) {
            schedule.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            schedule.setEndDate(request.getEndDate());
        }
        if (request.getReminderInApp() != null) {
            schedule.setReminderInApp(request.getReminderInApp());
        }
        if (request.getReminderEmail() != null) {
            schedule.setReminderEmail(request.getReminderEmail());
        }
        if (request.getReminderPush() != null) {
            schedule.setReminderPush(request.getReminderPush());
        }
        if (request.getActive() != null) {
            schedule.setActive(request.getActive());
        }
        if (request.getAdherenceAlertThreshold() != null) {
            schedule.setAdherenceAlertThreshold(request.getAdherenceAlertThreshold());
        }

        validateScheduleRequest(schedule.getScheduleType(), parseJsonList(schedule.getReminderTimes()),
                parseJsonList(schedule.getReminderDays()), schedule.getStartDate(), schedule.getEndDate());

        return toScheduleResponse(medicationScheduleRepository.save(schedule));
    }

    @Transactional
    public void deactivateSchedule(UUID patientUserId, UUID scheduleId) {
        Patient patient = getPatientByUserId(patientUserId);
        MedicationSchedule schedule = medicationScheduleRepository.findByIdAndPatientId(scheduleId, patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Medication schedule not found"));
        schedule.setActive(false);
        medicationScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public Page<MedicationDoseLogResponse> getDoseLogs(UUID patientUserId, Pageable pageable) {
        Patient patient = getPatientByUserId(patientUserId);
        return medicationDoseLogRepository.findByPatientIdOrderByScheduledAtDesc(patient.getId(), pageable)
                .map(this::toDoseLogResponse);
    }

    @Transactional
    public MedicationDoseLogResponse markDoseTaken(UUID patientUserId, UUID doseLogId, DoseActionRequest request) {
        Patient patient = getPatientByUserId(patientUserId);
        MedicationDoseLog doseLog = medicationDoseLogRepository.findByIdAndPatientId(doseLogId, patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dose log not found"));

        doseLog.setStatus(DoseStatus.TAKEN);
        doseLog.setTakenAt(LocalDateTime.now());
        if (request != null && request.getNote() != null) {
            doseLog.setNote(request.getNote());
        }

        return toDoseLogResponse(medicationDoseLogRepository.save(doseLog));
    }

    @Transactional
    public MedicationDoseLogResponse snoozeDose(UUID patientUserId, UUID doseLogId, DoseActionRequest request) {
        Patient patient = getPatientByUserId(patientUserId);
        MedicationDoseLog doseLog = medicationDoseLogRepository.findByIdAndPatientId(doseLogId, patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dose log not found"));

        int snoozeMinutes = request != null && request.getSnoozeMinutes() != null
                ? request.getSnoozeMinutes()
                : 15;

        doseLog.setStatus(DoseStatus.SNOOZED);
        doseLog.setSnoozedUntil(LocalDateTime.now().plusMinutes(snoozeMinutes));
        if (request != null && request.getNote() != null) {
            doseLog.setNote(request.getNote());
        }

        return toDoseLogResponse(medicationDoseLogRepository.save(doseLog));
    }

    @Transactional(readOnly = true)
    public MedicationAdherenceSummaryResponse getAdherenceSummary(UUID patientUserId, int days) {
        Patient patient = getPatientByUserId(patientUserId);
        return calculateAdherenceForPatient(patient, Math.max(days, 1));
    }

    @Transactional(readOnly = true)
    public List<DoctorAdherenceAlertResponse> getDoctorAdherenceAlerts(UUID doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        return medicationAdherenceAlertRepository.findByDoctorIdOrderByAlertedAtDesc(doctor.getId())
                .stream()
                .map(alert -> DoctorAdherenceAlertResponse.builder()
                        .alertId(alert.getId())
                        .patientId(alert.getPatient().getId())
                        .patientName(alert.getPatient().getName())
                        .adherencePercentage(alert.getAdherencePercentage())
                        .threshold(alert.getThreshold())
                        .alertedAt(alert.getAlertedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void processDueReminders() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        processSnoozedReminderQueue(now);

        List<MedicationSchedule> activeSchedules = medicationScheduleRepository.findActiveForDate(now.toLocalDate());
        for (MedicationSchedule schedule : activeSchedules) {
            if (!Boolean.TRUE.equals(schedule.getActive())) {
                continue;
            }
            if (!isScheduleDueToday(schedule, now.toLocalDate())) {
                continue;
            }

            List<LocalTime> times = parseTimes(schedule.getReminderTimes());
            for (LocalTime time : times) {
                if (time.getHour() != now.getHour() || time.getMinute() != now.getMinute()) {
                    continue;
                }
                LocalDateTime scheduledAt = now.toLocalDate().atTime(time);
                if (medicationDoseLogRepository.existsByScheduleIdAndScheduledAt(schedule.getId(), scheduledAt)) {
                    continue;
                }

                MedicationDoseLog logEntry = MedicationDoseLog.builder()
                        .schedule(schedule)
                        .patient(schedule.getPatient())
                        .doctor(schedule.getDoctor())
                        .scheduledAt(scheduledAt)
                        .status(DoseStatus.PENDING)
                        .reminderSent(true)
                        .build();
                medicationDoseLogRepository.save(logEntry);

                String title = "Medication Reminder";
                String message = "Time to take " + schedule.getMedicineName()
                        + (schedule.getDosage() != null ? " (" + schedule.getDosage() + ")" : "");

                notificationDispatchService.notifyUser(
                        schedule.getPatient().getUser(),
                        "MEDICATION_REMINDER",
                        title,
                        message,
                        logEntry.getId().toString(),
                        Boolean.TRUE.equals(schedule.getReminderInApp()),
                        Boolean.TRUE.equals(schedule.getReminderEmail()),
                        Boolean.TRUE.equals(schedule.getReminderPush()));
            }
        }
    }

    @Transactional
    public void reconcileMissedDoses() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(Math.max(missedDoseGraceMinutes, 5));
        List<MedicationDoseLog> overdue = medicationDoseLogRepository.findByStatusAndScheduledAtBefore(
                DoseStatus.PENDING, threshold);
        for (MedicationDoseLog doseLog : overdue) {
            doseLog.setStatus(DoseStatus.MISSED);
        }
        if (!overdue.isEmpty()) {
            medicationDoseLogRepository.saveAll(overdue);
        }
    }

    @Transactional
    public void processAdherenceAlerts() {
        List<MedicationSchedule> schedules = medicationScheduleRepository.findByActiveTrue();
        Map<UUID, List<MedicationSchedule>> groupedByPatient = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getPatient().getId()));

        for (List<MedicationSchedule> patientSchedules : groupedByPatient.values()) {
            if (patientSchedules.isEmpty()) {
                continue;
            }

            Patient patient = patientSchedules.get(0).getPatient();
            MedicationAdherenceSummaryResponse adherence = calculateAdherenceForPatient(patient, 30);
            double threshold = patientSchedules.stream()
                    .map(MedicationSchedule::getAdherenceAlertThreshold)
                    .filter(Objects::nonNull)
                    .min(Double::compareTo)
                    .orElse(defaultAdherenceThreshold);

            if (adherence.getAdherencePercentage() >= threshold) {
                continue;
            }

            Set<Doctor> doctors = patientSchedules.stream()
                    .map(MedicationSchedule::getDoctor)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (Doctor doctor : doctors) {
                if (shouldSkipDuplicateAlert(patient.getId(), doctor.getId())) {
                    continue;
                }

                MedicationAdherenceAlert alert = MedicationAdherenceAlert.builder()
                        .patient(patient)
                        .doctor(doctor)
                        .adherencePercentage(adherence.getAdherencePercentage())
                        .threshold(threshold)
                        .build();
                medicationAdherenceAlertRepository.save(alert);

                String title = "Low Medication Adherence Alert";
                String message = String.format(
                        "Patient %s adherence dropped to %.1f%% (threshold %.1f%%)",
                        patient.getName(), adherence.getAdherencePercentage(), threshold);

                notificationDispatchService.notifyUser(
                        doctor.getUser(),
                        "ADHERENCE_ALERT",
                        title,
                        message,
                        alert.getId().toString(),
                        true,
                        true,
                        true);
            }
        }
    }

    private void processSnoozedReminderQueue(LocalDateTime now) {
        List<MedicationDoseLog> snoozed = medicationDoseLogRepository
                .findByStatusAndSnoozedUntilLessThanEqual(DoseStatus.SNOOZED, now);

        for (MedicationDoseLog doseLog : snoozed) {
            doseLog.setStatus(DoseStatus.PENDING);
            doseLog.setScheduledAt(now);
            doseLog.setSnoozedUntil(null);
            medicationDoseLogRepository.save(doseLog);

            MedicationSchedule schedule = doseLog.getSchedule();
            String title = "Medication Reminder (Snoozed)";
            String message = "Reminder: take " + schedule.getMedicineName();
            notificationDispatchService.notifyUser(
                    schedule.getPatient().getUser(),
                    "MEDICATION_REMINDER",
                    title,
                    message,
                    doseLog.getId().toString(),
                    Boolean.TRUE.equals(schedule.getReminderInApp()),
                    Boolean.TRUE.equals(schedule.getReminderEmail()),
                    Boolean.TRUE.equals(schedule.getReminderPush()));
        }
    }

    private boolean shouldSkipDuplicateAlert(UUID patientId, UUID doctorId) {
        Optional<MedicationAdherenceAlert> latest = medicationAdherenceAlertRepository
                .findTopByPatientIdAndDoctorIdOrderByAlertedAtDesc(patientId, doctorId);
        if (latest.isEmpty()) {
            return false;
        }
        return latest.get().getAlertedAt().isAfter(LocalDateTime.now().minusHours(24));
    }

    private MedicationAdherenceSummaryResponse calculateAdherenceForPatient(Patient patient, int days) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(days);

        long taken = medicationDoseLogRepository.countByPatientIdAndScheduledAtBetweenAndStatus(
                patient.getId(), from, to, DoseStatus.TAKEN);
        long missed = medicationDoseLogRepository.countByPatientIdAndScheduledAtBetweenAndStatus(
                patient.getId(), from, to, DoseStatus.MISSED);
        long pending = medicationDoseLogRepository.countByPatientIdAndScheduledAtBetweenAndStatusIn(
                patient.getId(), from, to, List.of(DoseStatus.PENDING, DoseStatus.SNOOZED));

        long evaluated = taken + missed;
        double adherence = evaluated == 0 ? 100.0 : ((double) taken * 100.0) / evaluated;

        return MedicationAdherenceSummaryResponse.builder()
                .patientId(patient.getId())
                .adherencePercentage(Math.round(adherence * 100.0) / 100.0)
                .takenDoses(taken)
                .missedDoses(missed)
                .pendingDoses(pending)
                .totalEvaluatedDoses(evaluated)
                .from(from)
                .to(to)
                .build();
    }

    private Patient getPatientByUserId(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    private void validateScheduleRequest(
            ReminderScheduleType scheduleType,
            List<String> reminderTimes,
            List<String> reminderDays,
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (reminderTimes == null || reminderTimes.isEmpty()) {
            throw new IllegalArgumentException("At least one reminder time is required");
        }
        normaliseTimes(reminderTimes);

        if (scheduleType == ReminderScheduleType.CUSTOM && reminderDays != null && !reminderDays.isEmpty()) {
            normaliseDays(reminderDays);
        }
    }

    private List<String> normaliseTimes(List<String> reminderTimes) {
        if (reminderTimes == null || reminderTimes.isEmpty()) {
            return List.of();
        }
        return reminderTimes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(LocalTime::parse)
                .sorted()
                .map(LocalTime::toString)
                .distinct()
                .toList();
    }

    private List<String> normaliseDays(List<String> reminderDays) {
        if (reminderDays == null || reminderDays.isEmpty()) {
            return List.of();
        }
        return reminderDays.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .map(DayOfWeek::name)
                .distinct()
                .toList();
    }

    private boolean isScheduleDueToday(MedicationSchedule schedule, LocalDate date) {
        if (date.isBefore(schedule.getStartDate())) {
            return false;
        }
        if (schedule.getEndDate() != null && date.isAfter(schedule.getEndDate())) {
            return false;
        }

        if (schedule.getScheduleType() == ReminderScheduleType.DAILY) {
            return true;
        }
        if (schedule.getScheduleType() == ReminderScheduleType.ALTERNATE_DAY) {
            long daysSinceStart = ChronoUnit.DAYS.between(schedule.getStartDate(), date);
            return daysSinceStart % 2 == 0;
        }

        List<String> days = parseJsonList(schedule.getReminderDays());
        if (days.isEmpty()) {
            return true;
        }
        return days.contains(date.getDayOfWeek().name());
    }

    private List<LocalTime> parseTimes(String json) {
        return parseJsonList(json).stream()
                .map(LocalTime::parse)
                .sorted()
                .toList();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Invalid JSON list '{}': {}", json, e.getMessage());
            return List.of();
        }
    }

    private String toJsonList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize schedule metadata");
        }
    }

    private MedicationScheduleResponse toScheduleResponse(MedicationSchedule schedule) {
        return MedicationScheduleResponse.builder()
                .id(schedule.getId())
                .patientId(schedule.getPatient().getId())
                .doctorId(schedule.getDoctor() != null ? schedule.getDoctor().getId() : null)
                .prescriptionId(schedule.getPrescription() != null ? schedule.getPrescription().getId() : null)
                .medicineName(schedule.getMedicineName())
                .dosage(schedule.getDosage())
                .frequency(schedule.getFrequency())
                .instructions(schedule.getInstructions())
                .scheduleType(schedule.getScheduleType())
                .reminderTimes(parseJsonList(schedule.getReminderTimes()))
                .reminderDays(parseJsonList(schedule.getReminderDays()))
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .reminderInApp(schedule.getReminderInApp())
                .reminderEmail(schedule.getReminderEmail())
                .reminderPush(schedule.getReminderPush())
                .adherenceAlertThreshold(schedule.getAdherenceAlertThreshold())
                .active(schedule.getActive())
                .build();
    }

    private MedicationDoseLogResponse toDoseLogResponse(MedicationDoseLog doseLog) {
        return MedicationDoseLogResponse.builder()
                .id(doseLog.getId())
                .scheduleId(doseLog.getSchedule().getId())
                .medicineName(doseLog.getSchedule().getMedicineName())
                .scheduledAt(doseLog.getScheduledAt())
                .takenAt(doseLog.getTakenAt())
                .snoozedUntil(doseLog.getSnoozedUntil())
                .status(doseLog.getStatus())
                .note(doseLog.getNote())
                .build();
    }
}
