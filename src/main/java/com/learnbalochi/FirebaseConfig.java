package com.learnbalochi;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service.account.file:learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json}")
    private String serviceAccountFileName;

    @Value("${firebase.project.id:learnbalochi}")
    private String projectId;

    @Value("${firebase.database.url:}")
    private String databaseUrl;

    @PostConstruct
    public void initialize() {
        // Check if Firebase is already initialized to prevent multiple initialization errors
        if (FirebaseApp.getApps().size() > 0) {
            logger.info("Firebase is already initialized, skipping initialization");
            return;
        }

        try {
            // Try multiple possible paths for the Firebase service account file
            String[] possiblePaths = {
                "src/main/resources/" + serviceAccountFileName,
                "/app/src/main/resources/" + serviceAccountFileName,
                serviceAccountFileName
            };
            
            FileInputStream serviceAccount = null;
            for (String path : possiblePaths) {
                try {
                    serviceAccount = new FileInputStream(path);
                    logger.info("Found Firebase service account file at: {}", path);
                    break;
                } catch (IOException e) {
                    logger.debug("Firebase service account file not found at: {}", path);
                }
            }
            
            if (serviceAccount == null) {
                logger.error("Firebase service account file not found in any of the expected locations");
                return;
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId);

            // Set database URL if provided
            if (databaseUrl != null && !databaseUrl.trim().isEmpty()) {
                optionsBuilder.setDatabaseUrl(databaseUrl);
            }

            FirebaseOptions options = optionsBuilder.build();

            FirebaseApp.initializeApp(options);
            logger.info("Firebase initialized successfully for project: {}", projectId);
        } catch (IOException e) {
            logger.error("Error initializing Firebase", e);
        }
    }
}