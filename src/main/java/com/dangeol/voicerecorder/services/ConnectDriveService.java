package com.dangeol.voicerecorder.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class ConnectDriveService {
    private static final JacksonFactory jsonFactory = new JacksonFactory();
    private static final String CREDENTIALS_FILE_PATH = "/env.json";

    /**
     * Connects to Google Drive on behalf of a service account.
     * @return A Google Drive object.
     * @throws IOException If the file defined by CREDENTIALS_FILE_PATH cannot be found.
     */
    public Drive connect() throws IOException, GeneralSecurityException {

        InputStream in = ConnectDriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        GoogleCredentials credential = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singleton(DriveScopes.DRIVE));
        Drive drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory,
                new HttpCredentialsAdapter(credential))
                .setApplicationName("voicerecorder")
                .build();

        return drive;
    }
}
