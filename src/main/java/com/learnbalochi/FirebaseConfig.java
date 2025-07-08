package com.learnbalochi;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // Injects a comma-separated list of possible service account filenames from application.properties.
    // It will try each file in order until one is found in the classpath.
    // The property name is now plural to reflect this.
    @Value("${firebase.service.account.files:learnbalochi-prod-firebase-adminsdk-fbsvc-learnbaluchi.json}")
    private String[] serviceAccountFiles;

    @PostConstruct
    public void initialize() {
        // Prevent re-initialization
        if (!FirebaseApp.getApps().isEmpty()) {
            logger.info("Firebase has already been initialized.");
            return;
        }

        try {
            // Find the first available service account file from the list
            ClassPathResource resource = findFirebaseResource();

            if (resource == null) {
                logger.error("Firebase service account file not found in classpath. Searched for: {}", Arrays.toString(serviceAccountFiles));
                return;
            }

            logger.info("Found Firebase service account file at: {}", resource.getPath());

            try (InputStream serviceAccountStream = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully.");
            }

        } catch (IOException e) {
            logger.error("Error initializing Firebase.", e);
        }
    }

    /**
     * Searches for the first existing Firebase resource from the list of provided filenames.
     * @return A ClassPathResource if a file is found, otherwise null.
     */
    private ClassPathResource findFirebaseResource() {
        for (String file : serviceAccountFiles) {
            // Check if the file string is not null or empty
            if (StringUtils.hasText(file)) {
                // Trim whitespace from the filename
                ClassPathResource resource = new ClassPathResource(file.trim());
                if (resource.exists()) {
                    return resource;
                }
            }
        }
        return null; // Return null if no file is found
    }
}

// package com.learnbalochi;

// import com.google.auth.oauth2.GoogleCredentials;
// import com.google.firebase.FirebaseApp;
// import com.google.firebase.FirebaseOptions;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Configuration;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import javax.annotation.PostConstruct;
// import java.io.FileInputStream;
// import java.io.IOException;

// @Configuration
// public class FirebaseConfig {
//     private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

//     @Value("${firebase.service.account.file:learnbalochi-prod-firebase-adminsdk-fbsvc-learnbaluchi.json}")
//     private String serviceAccountFileName;

//     @Value("${firebase.project.id:learnbalochi}")
//     private String projectId;

//     @Value("${firebase.database.url:}")
//     private String databaseUrl;

//     @PostConstruct
//     public void initialize() {
//         // Check if Firebase is already initialized to prevent multiple initialization errors
//         if (FirebaseApp.getApps().size() > 0) {
//             logger.info("Firebase is already initialized, skipping initialization");
//             return;
//         }

//         try {
//             // Try multiple possible paths for the Firebase service account file
//             String[] possiblePaths = {
//                 "src/main/resources/" + serviceAccountFileName,
//                 "/app/src/main/resources/" + serviceAccountFileName,
//                 "/service/" + serviceAccountFileName,
//                 serviceAccountFileName
//             };
            
//             FileInputStream serviceAccount = null;
//             for (String path : possiblePaths) {
//                 try {
//                     serviceAccount = new FileInputStream(path);
//                     logger.info("Found Firebase service account file at: {}", path);
//                     break;
//                 } catch (IOException e) {
//                     logger.debug("Firebase service account file not found at: {}", path);
//                 }
//             }
            
//             if (serviceAccount == null) {
//                 logger.error("Firebase service account file not found in any of the expected locations");
//                 return;
//             }

//             FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
//                     .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                     .setProjectId(projectId);

//             // Set database URL if provided
//             if (databaseUrl != null && !databaseUrl.trim().isEmpty()) {
//                 optionsBuilder.setDatabaseUrl(databaseUrl);
//             }

//             FirebaseOptions options = optionsBuilder.build();

//             FirebaseApp.initializeApp(options);
//             logger.info("Firebase initialized successfully for project: {}", projectId);
//         } catch (IOException e) {
//             logger.error("Error initializing Firebase", e);
//         }
//     }
// }