package com.medsyncpro.utils;

import com.medsyncpro.entity.*;
import com.medsyncpro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class UserProfileHelper {
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final AdminRepository adminRepo;
    private final PharmacyRepository pharmacyRepo;
    private final AgentRepository agentRepo;
    private final VerificationRequestRepository verificationReqRepo;

    public String getName(User user) {
        if (user == null) return null;
        return switch (user.getRole()) {
            case DOCTOR -> doctorRepo.findByUserId(user.getId()).map(Doctor::getName).orElse(null);
            case PATIENT -> patientRepo.findByUser(user).map(Patient::getName).orElse(null);
            case ADMIN -> adminRepo.findByUser(user).map(Admin::getName).orElse(null);
            case PHARMACY -> pharmacyRepo.findByUser(user).map(Pharmacy::getName).orElse(null);
            case AGENT -> agentRepo.findByUser(user).map(Agent::getName).orElse(null);
            default -> null;
        };
    }

    public String getProfileImage(User user) {
        if (user == null) return null;
        return switch (user.getRole()) {
            case DOCTOR -> doctorRepo.findByUserId(user.getId()).map(Doctor::getProfileImage).orElse(null);
            case PATIENT -> patientRepo.findByUser(user).map(Patient::getProfileImage).orElse(null);
            case ADMIN -> adminRepo.findByUser(user).map(Admin::getProfileImage).orElse(null);
            case PHARMACY -> pharmacyRepo.findByUser(user).map(Pharmacy::getProfileImage).orElse(null);
            case AGENT -> agentRepo.findByUser(user).map(Agent::getProfileImage).orElse(null);
            default -> null;
        };
    }

    public LocalDate getDob(User user) {
        if (user == null) return null;
        if (user.getRole() == Role.PATIENT) {
            return patientRepo.findByUser(user).map(Patient::getDateOfBirth).orElse(null);
        }
        return null;
    }

    public String getAddress(User user) {
        if (user == null) return null;
        return switch (user.getRole()) {
            case DOCTOR -> doctorRepo.findByUserId(user.getId()).map(Doctor::getClinicAddress).orElse(null);
            case PATIENT -> patientRepo.findByUser(user).map(Patient::getAddress).orElse(null);
            case PHARMACY -> pharmacyRepo.findByUser(user).map(Pharmacy::getAddress).orElse(null);
            case AGENT -> agentRepo.findByUser(user).map(Agent::getAddress).orElse(null);
            default -> null;
        };
    }

    public com.medsyncpro.entity.Gender getGender(User user) {
        if (user == null) return null;
        if (user.getRole() == Role.PATIENT) {
            return patientRepo.findByUser(user).map(Patient::getGender).orElse(null);
        }
        return null;
    }

    public String getBloodGroup(User user) {
        if (user == null) return null;
        if (user.getRole() == Role.PATIENT) {
            return patientRepo.findByUser(user).map(Patient::getBloodGroup).orElse(null);
        }
        return null;
    }

    public String getBio(User user) {
        if (user == null) return null;
        if (user.getRole() == Role.DOCTOR) {
            return doctorRepo.findByUserId(user.getId()).map(Doctor::getBio).orElse(null);
        }
        return null;
    }

    public Integer getExperienceYears(User user) {
        if (user == null) return null;
        if (user.getRole() == Role.DOCTOR) {
            return doctorRepo.findByUserId(user.getId()).map(Doctor::getExperienceYears).orElse(null);
        }
        return null;
    }
    
    public String getCity(User user) { return null; }
    
    public String getState(User user) { return null; }

    public VerificationStatus getVerificationStatus(User user) {
        if (user == null) return VerificationStatus.UNVERIFIED;
        return verificationReqRepo.findByUserId(String.valueOf(user.getId()))
                .map(VerificationRequest::getStatus)
                .orElse(VerificationStatus.UNVERIFIED);
    }
}
