package com.example.tds.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:classpath:service-account-key.json}")
    private org.springframework.core.io.Resource firebaseConfigFile;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!firebaseConfigFile.exists()) {
            throw new IOException("Firebase service account file not found at " + firebaseConfigFile.getDescription());
        }
        InputStream serviceAccount = firebaseConfigFile.getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if(FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }

        return FirebaseApp.getInstance();
    }
}