package com.medsyncpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsyncpro.dto.request.AppointmentRequest;
import com.medsyncpro.dto.response.AppointmentResponse;
import com.medsyncpro.dto.response.SlotResponse;
import com.medsyncpro.entity.*;
import com.medsyncpro.event.AppointmentBookedEvent;
import com.medsyncpro.event.AppointmentCancelledEvent;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorSettingsRepository doctorSettingsRepository;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Book Appointment ─────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse bookAppointment(UUID patientUserId, AppointmentRequest request) {
        User patientUser = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient user not found"));

        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        DoctorSettings settings = doctorSettingsRepository.findByUserId(doctor.getUser().getId())
                .orElse(null);

        int slotDuration = settings != null ? settings.getSlotDurationMinutes() : 30;

        // Validate date is not in the past
        if (request.getScheduledDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book appointments in the past");
        }

        LocalTime endTime = request.getScheduledTime().plusMinutes(slotDuration);

        // Check for double-booking
        List<Appointment> conflicts = appointmentRepository.findConflicting(
                doctor.getId(),
                request.getScheduledDate(),
                request.getScheduledTime(),
                endTime);

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("This time slot is already booked. Please choose another slot.");
        }

        // Parse consultation type
        ConsultationType consultationType;
        try {
            consultationType = ConsultationType.valueOf(request.getType() != null ? request.getType() : "VIDEO");
        } catch (IllegalArgumentException e) {
            consultationType = ConsultationType.VIDEO;
        }

        // Determine initial status
        AppointmentStatus initialStatus = (settings != null
                && Boolean.TRUE.equals(settings.getAutoApproveAppointments()))
                        ? AppointmentStatus.CONFIRMED
                        : AppointmentStatus.PENDING;

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .scheduledDate(request.getScheduledDate())
                .scheduledTime(request.getScheduledTime())
                .endTime(endTime)
                .type(consultationType)
                .status(initialStatus)
                .symptoms(request.getSymptoms())
                .build();

        appointment = appointmentRepository.save(appointment);

        log.info("Appointment booked: {} by patient {} with doctor {}",
                appointment.getId(), patientUserId, doctor.getId());

        // Fire event for doctor notification
        eventPublisher.publishEvent(new AppointmentBookedEvent(this, appointment));

        return toResponse(appointment);
    }

    // ─── Patient Appointments ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getPatientAppointments(UUID patientUserId, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Page<Appointment> page = appointmentRepository
                .findByPatientIdOrderByScheduledDateDescScheduledTimeDesc(patient.getId(), pageable);

        List<AppointmentResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    // ─── Cancel Appointment (Patient) ────────────────────────────────────

    @Transactional
    public AppointmentResponse cancelAppointment(UUID patientUserId, UUID appointmentId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new IllegalArgumentException("You can only cancel your own appointments");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel this appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason("Cancelled by patient");
        appointmentRepository.save(appointment);

        eventPublisher.publishEvent(new AppointmentCancelledEvent(this, appointment, "PATIENT"));

        return toResponse(appointment);
    }

    // ─── Available Slots ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(UUID doctorId, String type) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        DoctorSettings settings = doctorSettingsRepository.findByUserId(doctor.getUser().getId())
                .orElse(null);

        if (settings == null || !Boolean.TRUE.equals(settings.getAvailableForConsultation())) {
            return List.of();
        }

        int slotDuration = settings.getSlotDurationMinutes();
        Map<String, Object> weeklySchedule = parseWeeklySchedule(settings.getWeeklySchedule());

        List<SlotResponse> slots = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Generate slots for the next 14 days
        for (int dayOffset = 0; dayOffset < 14; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayKey = dayOfWeek.name().substring(0, 1) + dayOfWeek.name().substring(1).toLowerCase();

            Map<String, Object> daySchedule = getDaySchedule(weeklySchedule, dayKey);
            if (daySchedule == null || !Boolean.TRUE.equals(daySchedule.get("enabled"))) {
                continue;
            }

            List<Map<String, String>> timeSlots = getTimeSlots(daySchedule);
            if (timeSlots == null || timeSlots.isEmpty()) {
                // Generate from start/end times if no explicit slots
                String startStr = (String) daySchedule.get("start");
                String endStr = (String) daySchedule.get("end");
                if (startStr != null && endStr != null) {
                    timeSlots = new ArrayList<>();
                    Map<String, String> slot = new HashMap<>();
                    slot.put("start", startStr);
                    slot.put("end", endStr);
                    timeSlots.add(slot);
                }
            }

            if (timeSlots == null)
                continue;

            // Get existing appointments for this date
            List<Appointment> existingAppts = appointmentRepository
                    .findByDoctorIdAndScheduledDate(doctor.getId(), date);

            Set<String> bookedTimes = existingAppts.stream()
                    .map(a -> a.getScheduledTime().toString())
                    .collect(Collectors.toSet());

            for (Map<String, String> timeSlot : timeSlots) {
                String start = timeSlot.get("start");
                String end = timeSlot.get("end");
                if (start == null || end == null)
                    continue;

                LocalTime slotStart = LocalTime.parse(start);
                LocalTime slotEnd = LocalTime.parse(end);

                while (slotStart.plusMinutes(slotDuration).compareTo(slotEnd) <= 0) {
                    // Skip slots in the past for today
                    if (date.equals(today) && slotStart.isBefore(LocalTime.now())) {
                        slotStart = slotStart.plusMinutes(slotDuration);
                        continue;
                    }

                    boolean isBooked = bookedTimes.contains(slotStart.toString());

                    slots.add(SlotResponse.builder()
                            .date(date.format(dateFormatter))
                            .time(slotStart.toString())
                            .available(!isBooked)
                            .build());

                    slotStart = slotStart.plusMinutes(slotDuration);
                }
            }
        }

        return slots;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseWeeklySchedule(String json) {
        if (json == null || json.isBlank()) {
            // Default schedule: Mon-Fri 09:00-17:00
            Map<String, Object> defaultSchedule = new LinkedHashMap<>();
            for (String day : new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" }) {
                Map<String, Object> dayMap = new LinkedHashMap<>();
                dayMap.put("enabled", true);
                dayMap.put("start", "09:00");
                dayMap.put("end", "17:00");
                defaultSchedule.put(day, dayMap);
            }
            for (String day : new String[] { "Saturday", "Sunday" }) {
                Map<String, Object> dayMap = new LinkedHashMap<>();
                dayMap.put("enabled", false);
                defaultSchedule.put(day, dayMap);
            }
            return defaultSchedule;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse weekly schedule: {}", e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDaySchedule(Map<String, Object> schedule, String dayKey) {
        Object val = schedule.get(dayKey);
        if (val instanceof Map)
            return (Map<String, Object>) val;
        // Try lowercase
        val = schedule.get(dayKey.toLowerCase());
        if (val instanceof Map)
            return (Map<String, Object>) val;
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getTimeSlots(Map<String, Object> daySchedule) {
        Object slots = daySchedule.get("slots");
        if (slots instanceof List) {
            return (List<Map<String, String>>) slots;
        }
        return null;
    }

    private AppointmentResponse toResponse(Appointment appt) {
        Doctor doctor = appt.getDoctor();
        Patient patient = appt.getPatient();
        User doctorUser = doctor.getUser();
        User patientUser = patient.getUser();

        String specialtyName = null;
        if (doctor.getSpecialization() != null) {
            specialtyName = doctor.getSpecialization().getName();
        } else {
            DoctorSettings settings = doctorSettingsRepository.findByUserId(doctorUser.getId()).orElse(null);
            if (settings != null)
                specialtyName = settings.getSpecialty();
        }

        return AppointmentResponse.builder()
                .id(appt.getId())
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .doctorSpecialty(specialtyName)
                .doctorProfileImage(doctor.getProfileImage())
                .patientId(patient.getId())
                .patientName(patient.getName())
                .patientEmail(patientUser.getEmail())
                .patientPhone(patientUser.getPhone())
                .scheduledDate(appt.getScheduledDate())
                .scheduledTime(appt.getScheduledTime())
                .endTime(appt.getEndTime())
                .type(appt.getType().name())
                .status(appt.getStatus().name())
                .symptoms(appt.getSymptoms())
                .doctorNotes(appt.getDoctorNotes())
                .diagnosis(appt.getDiagnosis())
                .followUpDate(appt.getFollowUpDate())
                .cancellationReason(appt.getCancellationReason())
                .prescription(appt.getPrescription())
                .build();
    }
}
