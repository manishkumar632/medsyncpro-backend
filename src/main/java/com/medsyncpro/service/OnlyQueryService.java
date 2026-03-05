package com.medsyncpro.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medsyncpro.dto.response.AdminStatsResponse;
import com.medsyncpro.dto.response.DocumentTypeResponse;
import com.medsyncpro.entity.Role;
import com.medsyncpro.repository.AgentRepository;
import com.medsyncpro.repository.DoctorRepository;
import com.medsyncpro.repository.DocumentTypeRepository;
import com.medsyncpro.repository.PharmacyRepository;
import com.medsyncpro.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OnlyQueryService {
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AgentRepository agentRepository;
    private final PharmacyRepository pharmacyRepository;
    private final DocumentTypeRepository documentTypeRepository;


    public AdminStatsResponse getStats() {

            long totalPatients = userRepository.countByRoleAndDeletedFalse(Role.PATIENT);

            long verifiedDoctors = doctorRepository.countByIsVerifiedTrueAndDeletedFalse();
            long unverifiedDoctors = doctorRepository.countByIsVerifiedFalseAndDeletedFalse();

            long verifiedPharmacists = pharmacyRepository.countByIsVerifiedTrueAndDeletedFalse();
            long unverifiedPharmacists = pharmacyRepository.countByIsVerifiedFalseAndDeletedFalse();

            long verifiedAgents = agentRepository.countByIsVerifiedTrueAndDeletedFalse();
            long unverifiedAgents = agentRepository.countByIsVerifiedFalseAndDeletedFalse();

            return AdminStatsResponse.builder()
                            .totalPatients(totalPatients)
                            .doctors(AdminStatsResponse.RoleStats.builder()
                                            .verified(verifiedDoctors)
                                            .unverified(unverifiedDoctors)
                                            .build())
                            .pharmacists(AdminStatsResponse.RoleStats.builder()
                                            .verified(verifiedPharmacists)
                                            .unverified(unverifiedPharmacists)
                                            .build())
                            .agents(AdminStatsResponse.RoleStats.builder()
                                            .verified(verifiedAgents)
                                            .unverified(unverifiedAgents)
                                            .build())
                            .build();
    }

    public List<DocumentTypeResponse> getAllDocumentTypes() {
        return documentTypeRepository.findAll()
                .stream()
                .map(DocumentTypeResponse::from)
                .toList();
    }

}
