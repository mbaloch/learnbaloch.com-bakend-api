package com.learnbalochi;

import com.google.auth.oauth2.GoogleCredentials;
     import com.google.firebase.FirebaseApp;
     import com.google.firebase.FirebaseOptions;
     import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
     import java.io.FileInputStream;
     import java.io.IOException;

     @Configuration
     public class FirebaseConfig {
         private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

         @PostConstruct
         public void initialize() {
             try {
                 // Try multiple possible paths for the Firebase service account file
                 String[] possiblePaths = {
                     "src/main/resources/learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json",
                     "/app/src/main/resources/learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json",
                     "learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json"
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

                 FirebaseOptions options = FirebaseOptions.builder()
                         .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                         .setDatabaseUrl("https://<your-database-name>.firebaseio.com")
                         .build();

                 FirebaseApp.initializeApp(options);
                 logger.info("Firebase initialized successfully");
             } catch (IOException e) {
                 logger.error("Error initializing Firebase", e);
             }
         }
     }