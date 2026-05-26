package com.example.demo.config;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {

        try {

        	FileInputStream serviceAccount =
        	        new FileInputStream(
        	        "src/main/resources/bharatsarthi-e8440-firebase-adminsdk-fbsvc-c10a6ba473.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {

                FirebaseApp.initializeApp(options);

                System.out.println("Firebase initialized");

            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
