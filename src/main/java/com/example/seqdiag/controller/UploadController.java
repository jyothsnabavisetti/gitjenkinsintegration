package com.example.seqdiag.controller;

import com.example.seqdiag.service.SequenceExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final SequenceExtractor extractor;

    public UploadController(SequenceExtractor extractor) {
        this.extractor = extractor;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadZip(@RequestParam("file") MultipartFile file) {
        try {
            String plantuml = extractor.extractPlantUmlFromZip(file);
            byte[] png = extractor.renderPlantUmlToPng(plantuml);
            String pngBase64 = Base64.getEncoder().encodeToString(png);
            return ResponseEntity.ok(Map.of("plantuml", plantuml, "pngBase64", pngBase64));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}