package com.learnbalochi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1")
public class TranslateController {
    private static final Logger logger = LoggerFactory.getLogger(TranslateController.class);
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    private static final String TRANSLATION_API_URL = "http://translator:9000/v1/chat/completions";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MyFirestoreService firestoreService; // Use your MyFirestoreService to save data

    @PostMapping("/translate")
    public ResponseEntity<Object> translateText(@RequestBody String text) throws ExecutionException, InterruptedException {
        logger.info("Translate request received for text: {}", text);
        
        // First, check if translation already exists in Firestore
        Map<String, Object> existingTranslation = firestoreService.findTranslationByOriginalText(text);
        
        if (existingTranslation != null) {
            logger.info("Found existing translation for text: {}", text);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("originalText", existingTranslation.get("originalText"));
            responseBody.put("translatedText", existingTranslation.get("translatedText"));
            responseBody.put("fromCache", true);
            responseBody.put("documentId", existingTranslation.get("documentId"));
            return ResponseEntity.ok(responseBody);
        }
        
        logger.info("No existing translation found, making new translation request");
        
        // If no existing translation, proceed with new translation
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "balochi-translator");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You are a helpful assistant that translates English sentences into Balochi."
        ));
        messages.add(Map.of(
                "role", "user",
                "content", "Translate: " + text
        ));

        requestBody.put("messages", messages);
        requestBody.put("max_completion_tokens", 256);
        requestBody.put("temperature", 0.6);
        requestBody.put("use_beam_search", true);
        requestBody.put("top_p", 0.9);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(TRANSLATION_API_URL, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> choices = (Map<String, Object>) ((List<?>) response.getBody().get("choices")).get(0);
            Map<String, Object> message = (Map<String, Object>) choices.get("message");
            String rawTranslatedText = message.get("content").toString();
            
            // Extract the actual translated text consistently
            String translatedText = extractTranslatedText(rawTranslatedText);

            Map<String, Object> translationData = new HashMap<>();
            translationData.put("originalText", text);
            translationData.put("translatedText", translatedText);
            translationData.put("timestamp", System.currentTimeMillis());

            String documentId = firestoreService.addDocumentToCollection("Translations", translationData);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("originalText", text);
            responseBody.put("translatedText", translatedText);
            responseBody.put("fromCache", false);
            responseBody.put("documentId", documentId);
            return ResponseEntity.ok(responseBody);
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Translation failed");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Extracts the actual translated text from the API response
     * Handles both JSON-wrapped responses {"text":"..."} and plain text responses
     */
    private String extractTranslatedText(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return rawResponse;
        }
        
        String response = rawResponse.trim();
        logger.debug("Original response: '{}'", response);
        
        // Remove "Balochi: " prefix if present
        if (response.startsWith("Balochi: ")) {
            response = response.substring("Balochi: ".length());
            logger.debug("After removing 'Balochi: ' prefix: '{}'", response);
        }
        
        // Check if the response is wrapped in JSON format {"text":"..."}
        if (response.startsWith("{\"text\":\"") && response.endsWith("\"}")) {
            try {
                // Extract the text value from the JSON
                String jsonText = response.substring(8, response.length() - 2); // Remove {"text":" and "}
                logger.debug("Extracted from JSON: '{}'", jsonText);
                
                // Clean any remaining quotes from the extracted JSON text
                while (jsonText.startsWith("\"")) {
                    jsonText = jsonText.substring(1);
                }
                while (jsonText.endsWith("\"")) {
                    jsonText = jsonText.substring(0, jsonText.length() - 1);
                }
                
                logger.debug("After cleaning JSON text: '{}'", jsonText);
                return jsonText.trim();
            } catch (Exception e) {
                logger.warn("Failed to parse JSON-wrapped translation response: {}", response, e);
                return response;
            }
        }
        
        // Remove multiple quotes at the beginning
        int startQuotes = 0;
        while (response.startsWith("\"")) {
            response = response.substring(1);
            startQuotes++;
        }
        logger.debug("Removed {} quotes from start: '{}'", startQuotes, response);
        
        // Remove multiple quotes at the end
        int endQuotes = 0;
        while (response.endsWith("\"")) {
            response = response.substring(0, response.length() - 1);
            endQuotes++;
        }
        logger.debug("Removed {} quotes from end: '{}'", endQuotes, response);
        
        // Final trim and return
        String finalResult = response.trim();
        logger.debug("Final cleaned text: '{}'", finalResult);
        return finalResult;
    }
}