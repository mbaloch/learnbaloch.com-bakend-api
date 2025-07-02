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
                 FileInputStream serviceAccount =
                         new FileInputStream("src/main/resources/learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json");

                 FirebaseOptions options = FirebaseOptions.builder()
                         .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                         .setDatabaseUrl("https://<your-database-name>.firebaseio.com")
                         .build();

                 FirebaseApp.initializeApp(options);
             } catch (IOException e) {
                 logger.error("Error initializing Firebase", e);
             }
         }
     }