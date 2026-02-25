package com.medsyncpro.service;

import com.medsyncpro.dto.AdminStatsResponse;
import com.medsyncpro.entity.Role;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public AdminStatsResponse getUserStats() {
        long patients = userRepository.countByRoleAndDeletedFalse(Role.PATIENT);
        long doctors = userRepository.countByRoleAndDeletedFalse(Role.DOCTOR);
        long pharmacists = userRepository.countByRoleAndDeletedFalse(Role.PHARMACIST);
        long pendingApprovals = userRepository.countPendingApprovals();
        long totalUsers = patients + doctors + pharmacists;
        
        log.info("Admin stats fetched — patients:{}, doctors:{}, pharmacists:{}, pending:{}", 
                patients, doctors, pharmacists, pendingApprovals);
        
        return new AdminStatsResponse(patients, doctors, pharmacists, totalUsers, pendingApprovals);
    }
}
