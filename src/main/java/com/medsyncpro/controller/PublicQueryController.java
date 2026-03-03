package com.medsyncpro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medsyncpro.dto.response.AdminStatsResponse;
import com.medsyncpro.service.OnlyQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicQueryController {
    private final OnlyQueryService queryService;

}
