package com.learnbalochi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    @Autowired
    private MyFirestoreService firestoreService;

    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> feedback) {
        try {
            String documentId = firestoreService.addDocumentToCollection("feedback", feedback);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("id", documentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to save feedback");
            return ResponseEntity.status(500).body(error);
        }
    }
} 