package com.medsyncpro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
public class FileController {
    
    @GetMapping("/**")
    public ResponseEntity<String> redirectToCloudinary() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header("Location", "https://res.cloudinary.com/dhopew3ev/")
                .body("Files are now served from Cloudinary");
    }
}
