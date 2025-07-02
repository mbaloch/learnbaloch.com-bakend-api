package com.learnbalochi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/ifiles")
public class MyCollectionController {
    String collectionName = "inpage_converted_files";

    @Autowired
    private MyFirestoreService firestoreService;

    @GetMapping
    public PaginatedResponse getAllDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws InterruptedException, ExecutionException {
        // Validate pagination parameters
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10; // Limit max page size to 100

        return firestoreService.getAllDocumentsFromCollectionPaginated(collectionName, page, size);
    }

    @PostMapping
    public String addDocument(@RequestBody Map<String, Object> data) throws InterruptedException, ExecutionException {
        firestoreService.addDocumentToCollection(collectionName, data);
        return "Document added successfully";
    }

    @PutMapping("/{documentId}")
    public String updateDocument(@PathVariable String documentId, @RequestBody Map<String, Object> updates) throws InterruptedException, ExecutionException {
        firestoreService.updateDocumentInCollection(collectionName, documentId, updates);
        return "Document updated successfully";
    }

    @GetMapping("/{documentId}")
    public Map<String, Object> getDocumentById(@PathVariable String documentId) throws InterruptedException, ExecutionException {
        return firestoreService.getDocumentById(collectionName, documentId);
    }

    @DeleteMapping("/{documentId}")
    public String deleteDocument(@PathVariable String documentId) {
        firestoreService.deleteDocumentInCollection(collectionName, documentId);
        return "Document deleted successfully";
    }

    @GetMapping("/search")
    public PaginatedResponse searchFiles(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws InterruptedException, ExecutionException {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        return firestoreService.searchAllDocumentsFromCollection(collectionName, query, page, size);
    }
}

