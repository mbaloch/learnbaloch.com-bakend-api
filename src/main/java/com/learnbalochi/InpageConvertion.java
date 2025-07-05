package com.learnbalochi;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/files")
public class InpageConvertion {
    private static final Logger logger = LoggerFactory.getLogger(InpageConvertion.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private MyFirestoreService firestoreService; // Use your MyFirestoreService to save data
    String collectionName = "inpage_converted_files";
    
    @Value("${inpage.convertor.url}")
    private String inpageConvertorApiUrl;

    @GetMapping("/list")
    public PaginatedResponse listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userUid
    ) throws ExecutionException, InterruptedException {
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10; // Limit max page size to 100

        return firestoreService.getDocumentsWithPrivacyFilter(collectionName, page, size, userUid);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocumentById(@PathVariable String documentId) throws ExecutionException, InterruptedException {
        Map<String, Object> document = firestoreService.getDocumentById(collectionName, documentId);
        
        if (document != null) {
            return ResponseEntity.ok(document);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadInpage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("meta") String metaJson
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            FileMetaDTO meta = mapper.readValue(metaJson, FileMetaDTO.class);
            String collectionName = "inpage_converted_files";

            logger.info("Uploaded inpage file received");

            // Step 1: Prepare the file to be forwarded to the external API
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Use LinkedMultiValueMap for multipart/form-data
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            logger.info("Calling inpage convertor API at {}...", inpageConvertorApiUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(inpageConvertorApiUrl, requestEntity, Map.class);
            logger.info("Inpage convertor API response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("content")) {
                    String unicodeText = responseBody.get("content").toString();

                    // Step 4: Save the data (metadata + contents) to Firebase
                    Map<String, Object> dataToSave = new HashMap<>();
                    dataToSave.put("fileName", meta.fileName());
                    dataToSave.put("authorName", meta.authors());
                    dataToSave.put("description", meta.fileDescription());
                    dataToSave.put("category", meta.category());
                    dataToSave.put("content", unicodeText);
                    dataToSave.put("isPublic", meta.isPublic());
                    dataToSave.put("uploaderEmail", meta.uploaderEmail());
                    dataToSave.put("uploaderUid", meta.uploaderUid());
                    dataToSave.put("uploadDate", System.currentTimeMillis());

                    try {
                        String documentId = firestoreService.addDocumentToCollection(collectionName, dataToSave); // Save using MyFirestoreService

                        // Add the document ID to the response
                        Map<String, Object> responseData = new HashMap<>(dataToSave);
                        responseData.put("documentId", documentId);

                        return new ResponseEntity<>(responseData, HttpStatus.OK);
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Error while saving data to Firestore", e);
                        throw new RuntimeException("Error while saving data to Firestore", e);
                    }
                } else {
                    logger.error("Convertor API response missing 'content': {}", responseBody);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                logger.error("Inpage convertor API returned non-OK status: {}", response.getStatusCode());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            logger.error("Error during file upload", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Upload failed: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public PaginatedResponse searchFiles(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userUid
    ) throws ExecutionException, InterruptedException {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        return firestoreService.searchDocumentsWithPrivacyFilter(collectionName, query, page, size, userUid);
    }
}
