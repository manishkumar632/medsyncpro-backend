package com.medsyncpro.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medsyncpro.dto.response.DocumentTypeResponse;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.OnlyQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicQueryController {
    private final OnlyQueryService queryService;

    @GetMapping("/get-all-document-types")
    public ResponseEntity<ApiResponse<List<DocumentTypeResponse>>> getAllDocumentTypes() {
        List<DocumentTypeResponse> docs = queryService.getAllDocumentTypes();
        
        return ResponseEntity.ok(
                ApiResponse.success(docs, "Document types fetched successfully"));
        }
}
