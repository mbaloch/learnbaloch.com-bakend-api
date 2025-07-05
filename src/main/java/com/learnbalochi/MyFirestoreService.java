package com.learnbalochi;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@Service
public class MyFirestoreService {

    public List<Map<String, Object>> getAllDocumentsFromCollection(String collectionName) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        ApiFuture<QuerySnapshot> querySnapshot = collection.get();

        List<Map<String, Object>> documents = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null) {
                // Add the document ID to the data
                documentData.put("documentId", document.getId());
                documents.add(documentData);
            }
        }

        return documents;
    }

    public String addDocumentToCollection(String collectionName, Map<String, Object> data) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        ApiFuture<DocumentReference> addedDocRef = collection.add(data);
        String documentId = addedDocRef.get().getId();
        System.out.println("Added document with ID: " + documentId);
        return documentId;
    }

    public void updateDocumentInCollection(String collectionName, String documentId, Map<String, Object> updates) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        ApiFuture<WriteResult> writeResult = docRef.update(updates);
        System.out.println("Update time : " + writeResult.get().getUpdateTime());
    }

    public void deleteDocumentInCollection(String collectionName, String documentId) {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<WriteResult> writeResult = db.collection(collectionName).document(documentId).delete();
    }

    public List<Map<String, Object>> getAllDocumentsWithoutContent(String collectionName) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        ApiFuture<QuerySnapshot> querySnapshot = collection.get();

        List<Map<String, Object>> documents = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null) {
                // Create a new map without the "content" field
                Map<String, Object> documentWithoutContent = new HashMap<>(documentData);
                documentWithoutContent.remove("content");
                // Add the document ID to the data
                documentWithoutContent.put("documentId", document.getId());
                documents.add(documentWithoutContent);
            }
        }

        return documents;
    }

    public PaginatedResponse getDocumentsWithoutContentPaginated(String collectionName, int page, int size) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        
        // Get total count first
        ApiFuture<QuerySnapshot> countQuery = collection.get();
        long totalElements = countQuery.get().size();
        
        // Calculate offset
        int offset = (page - 1) * size;
        
        // Get paginated documents
        Query query = collection.orderBy("fileName").limit(size).offset(offset);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        List<Map<String, Object>> documents = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null) {
                // Create a new map without the "content" field
                Map<String, Object> documentWithoutContent = new HashMap<>(documentData);
                documentWithoutContent.remove("content");
                // Add the document ID to the data
                documentWithoutContent.put("documentId", document.getId());
                documents.add(documentWithoutContent);
            }
        }

        PaginatedResponse.PaginationMeta paginationMeta = new PaginatedResponse.PaginationMeta(page, size, totalElements);
        return new PaginatedResponse(documents, paginationMeta);
    }

    public PaginatedResponse getAllDocumentsFromCollectionPaginated(String collectionName, int page, int size) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        
        // Get total count first
        ApiFuture<QuerySnapshot> countQuery = collection.get();
        long totalElements = countQuery.get().size();
        
        // Calculate offset
        int offset = (page - 1) * size;
        
        // Get paginated documents
        Query query = collection.orderBy("fileName").limit(size).offset(offset);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        List<Map<String, Object>> documents = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null) {
                // Add the document ID to the data
                documentData.put("documentId", document.getId());
                documents.add(documentData);
            }
        }

        PaginatedResponse.PaginationMeta paginationMeta = new PaginatedResponse.PaginationMeta(page, size, totalElements);
        return new PaginatedResponse(documents, paginationMeta);
    }

    public PaginatedResponse searchDocumentsWithoutContent(String collectionName, String searchQuery, int page, int size) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        
        // Get all documents first (since Firestore doesn't support full-text search natively)
        ApiFuture<QuerySnapshot> querySnapshot = collection.get();
        List<QueryDocumentSnapshot> allDocuments = querySnapshot.get().getDocuments();
        
        // Filter documents based on search query
        List<Map<String, Object>> filteredDocuments = new ArrayList<>();
        String lowerSearchQuery = searchQuery.toLowerCase();
        
        for (DocumentSnapshot document : allDocuments) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null && matchesSearch(documentData, lowerSearchQuery)) {
                // Create a new map without the "content" field
                Map<String, Object> documentWithoutContent = new HashMap<>(documentData);
                documentWithoutContent.remove("content");
                // Add the document ID to the data
                documentWithoutContent.put("documentId", document.getId());
                filteredDocuments.add(documentWithoutContent);
            }
        }
        
        // Calculate pagination
        long totalElements = filteredDocuments.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, filteredDocuments.size());
        
        // Get paginated subset
        List<Map<String, Object>> paginatedDocuments = new ArrayList<>();
        if (startIndex < filteredDocuments.size()) {
            paginatedDocuments = filteredDocuments.subList(startIndex, endIndex);
        }
        
        PaginatedResponse.PaginationMeta paginationMeta = new PaginatedResponse.PaginationMeta(page, size, totalElements);
        return new PaginatedResponse(paginatedDocuments, paginationMeta);
    }

    public PaginatedResponse searchAllDocumentsFromCollection(String collectionName, String searchQuery, int page, int size) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        
        // Get all documents first (since Firestore doesn't support full-text search natively)
        ApiFuture<QuerySnapshot> querySnapshot = collection.get();
        List<QueryDocumentSnapshot> allDocuments = querySnapshot.get().getDocuments();
        
        // Filter documents based on search query
        List<Map<String, Object>> filteredDocuments = new ArrayList<>();
        String lowerSearchQuery = searchQuery.toLowerCase();
        
        for (DocumentSnapshot document : allDocuments) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null && matchesSearch(documentData, lowerSearchQuery)) {
                // Add the document ID to the data
                documentData.put("documentId", document.getId());
                filteredDocuments.add(documentData);
            }
        }
        
        // Calculate pagination
        long totalElements = filteredDocuments.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, filteredDocuments.size());
        
        // Get paginated subset
        List<Map<String, Object>> paginatedDocuments = new ArrayList<>();
        if (startIndex < filteredDocuments.size()) {
            paginatedDocuments = filteredDocuments.subList(startIndex, endIndex);
        }
        
        PaginatedResponse.PaginationMeta paginationMeta = new PaginatedResponse.PaginationMeta(page, size, totalElements);
        return new PaginatedResponse(paginatedDocuments, paginationMeta);
    }

    private boolean matchesSearch(Map<String, Object> documentData, String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return true;
        }
        
        // Search in fileName
        if (documentData.containsKey("fileName")) {
            String fileName = String.valueOf(documentData.get("fileName"));
            if (fileName.toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        
        // Search in authorName (handle both array and string)
        if (documentData.containsKey("authorName")) {
            Object authorNameObj = documentData.get("authorName");
            if (authorNameObj instanceof List) {
                List<?> authors = (List<?>) authorNameObj;
                for (Object author : authors) {
                    if (String.valueOf(author).toLowerCase().contains(searchQuery)) {
                        return true;
                    }
                }
            } else if (authorNameObj instanceof String) {
                if (String.valueOf(authorNameObj).toLowerCase().contains(searchQuery)) {
                    return true;
                }
            }
        }
        
        // Search in description
        if (documentData.containsKey("description")) {
            String description = String.valueOf(documentData.get("description"));
            if (description.toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        
        // Search in category
        if (documentData.containsKey("category")) {
            String category = String.valueOf(documentData.get("category"));
            if (category.toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        
        // Search in content (if available)
        if (documentData.containsKey("content")) {
            String content = String.valueOf(documentData.get("content"));
            if (content.toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        
        return false;
    }

    public Map<String, Object> getDocumentById(String collectionName, String documentId) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(collectionName).document(documentId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        
        DocumentSnapshot document = documentSnapshot.get();
        if (document.exists()) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null) {
                // Add the document ID to the data
                documentData.put("documentId", document.getId());
            }
            return documentData;
        } else {
            return null; // Document doesn't exist
        }
    }

    public Map<String, Object> findTranslationByOriginalText(String originalText) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection("Translations");
        
        // Query for documents where originalText equals the provided text
        Query query = collection.whereEqualTo("originalText", originalText);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        
        if (!documents.isEmpty()) {
            // Return the first matching document (assuming unique translations)
            Map<String, Object> documentData = documents.get(0).getData();
            if (documentData != null) {
                documentData.put("documentId", documents.get(0).getId());
            }
            return documentData;
        }
        
        return null; // No translation found
    }

    public PaginatedResponse getDocumentsWithPrivacyFilter(String collectionName, int page, int size, String currentUserUid) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        
        // Get all documents first
        ApiFuture<QuerySnapshot> querySnapshot = collection.get();
        List<QueryDocumentSnapshot> allDocuments = querySnapshot.get().getDocuments();
        
        // Filter documents based on privacy settings
        List<Map<String, Object>> filteredDocuments = new ArrayList<>();
        for (DocumentSnapshot document : allDocuments) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null && isDocumentAccessible(documentData, currentUserUid)) {
                // Create a new map without the "content" field
                Map<String, Object> documentWithoutContent = new HashMap<>(documentData);
                documentWithoutContent.remove("content");
                // Add the document ID to the data
                documentWithoutContent.put("documentId", document.getId());
                filteredDocuments.add(documentWithoutContent);
            }
        }
        
        // Calculate pagination
        long totalElements = filteredDocuments.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, filteredDocuments.size());
        
        // Get paginated subset
        List<Map<String, Object>> paginatedDocuments = new ArrayList<>();
        if (startIndex < filteredDocuments.size()) {
            paginatedDocuments = filteredDocuments.subList(startIndex, endIndex);
        }
        
        PaginatedResponse.PaginationMeta paginationMeta = new PaginatedResponse.PaginationMeta(page, size, totalElements);
        return new PaginatedResponse(paginatedDocuments, paginationMeta);
    }

    public PaginatedResponse searchDocumentsWithPrivacyFilter(String collectionName, String searchQuery, int page, int size, String currentUserUid) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collection = db.collection(collectionName);
        
        // Get all documents first
        ApiFuture<QuerySnapshot> querySnapshot = collection.get();
        List<QueryDocumentSnapshot> allDocuments = querySnapshot.get().getDocuments();
        
        // Filter documents based on search query and privacy settings
        List<Map<String, Object>> filteredDocuments = new ArrayList<>();
        String lowerSearchQuery = searchQuery.toLowerCase();
        
        for (DocumentSnapshot document : allDocuments) {
            Map<String, Object> documentData = document.getData();
            if (documentData != null && isDocumentAccessible(documentData, currentUserUid) && matchesSearch(documentData, lowerSearchQuery)) {
                // Create a new map without the "content" field
                Map<String, Object> documentWithoutContent = new HashMap<>(documentData);
                documentWithoutContent.remove("content");
                // Add the document ID to the data
                documentWithoutContent.put("documentId", document.getId());
                filteredDocuments.add(documentWithoutContent);
            }
        }
        
        // Calculate pagination
        long totalElements = filteredDocuments.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, filteredDocuments.size());
        
        // Get paginated subset
        List<Map<String, Object>> paginatedDocuments = new ArrayList<>();
        if (startIndex < filteredDocuments.size()) {
            paginatedDocuments = filteredDocuments.subList(startIndex, endIndex);
        }
        
        PaginatedResponse.PaginationMeta paginationMeta = new PaginatedResponse.PaginationMeta(page, size, totalElements);
        return new PaginatedResponse(paginatedDocuments, paginationMeta);
    }

    private boolean isDocumentAccessible(Map<String, Object> documentData, String currentUserUid) {
        // If no user is logged in, only show public documents
        if (currentUserUid == null || currentUserUid.isEmpty()) {
            Boolean isPublic = (Boolean) documentData.get("isPublic");
            return isPublic != null && isPublic;
        }
        
        // If user is logged in, show public documents and their own private documents
        Boolean isPublic = (Boolean) documentData.get("isPublic");
        String uploaderUid = (String) documentData.get("uploaderUid");
        
        return (isPublic != null && isPublic) || currentUserUid.equals(uploaderUid);
    }
}
