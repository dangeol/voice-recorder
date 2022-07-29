package com.dangeol.voicerecorder.utils;

import com.dangeol.voicerecorder.VoiceRecorder;
import com.dangeol.voicerecorder.listeners.UploadProgressListener;
import com.dangeol.voicerecorder.services.ConnectDriveService;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UploadUtil {

    private static final Logger logger = LoggerFactory.getLogger(UploadUtil.class);
    private static final MessageUtil messages = new MessageUtil();

    private final ConnectDriveService connectDriveService = new ConnectDriveService();

    /**
     * Upload the mp3 to Google Drive
     * @param messageChannel
     * @throws IOException
     */
    public void uploadMp3(MessageChannel messageChannel) throws IOException {
        String originalFileName = getFileName();
        // Construct final filename for the uploaded file:
        String fileName = originalFileName.substring(0, 14)+"_"+messageChannel.getName()+".mp3";
        File mp3File = new File("mp3/"+originalFileName);

        try (FileInputStream fileInputStream = new FileInputStream(mp3File);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            Drive drive = connectDriveService.connect();
            com.google.api.services.drive.model.File file = new com.google.api.services.drive.model.File();
            UploadProgressListener uploadProgressListener = new UploadProgressListener();
            InputStreamContent mediaContent = new InputStreamContent("audio/mpeg", bufferedInputStream );
            mediaContent.setLength(mp3File.length());
            // Save the file in a shared folder whose ID is the value of "upload_folder_id"
            file.setParents(Collections.singletonList(VoiceRecorder.getEnvItem("upload_folder_id")));
            file.setName(fileName);
            Drive.Files.Create request = drive.files().create(file, mediaContent);
            MediaHttpUploader uploader = request.getMediaHttpUploader();
            uploader.setProgressListener(uploadProgressListener);
            request.execute();
            if (uploader.getProgress() == 1.0) {
                String link = getLink(drive, fileName);
                messages.onUploadComplete(messageChannel, link);
                try {
                    Files.deleteIfExists(Paths.get("mp3/"+originalFileName));
                } catch(IOException e) {
                    logger.error(e.getMessage());
                }
            };
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Get the name of the mp3 file that was just saved
     * @return: the last file in the mp3 folder
     */
    private String getFileName() {
        List<String> listOfFiles = new ArrayList<String>();
        try (Stream<Path> walk = Files.walk(Paths.get("mp3"))) {
            listOfFiles = walk.filter(Files::isRegularFile)
                    .map(x -> x.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        if (listOfFiles.size() > 1) {
            logger.warn("There are more than one files in the mp3 folder!");
        }
        return listOfFiles.get(listOfFiles.size() - 1);
    }

    /**
     * Get the download link of the mp3 file.
     * @Todo: Is there an easier way to set file Metadata in Google Drive API v3?
     * @param drive
     * @param fileName
     * @return: String of the download link
     * @throws IOException
     */
    private String getLink(Drive drive, String fileName) throws IOException {
        String link = "The link is not available";
        FileList driveFiles = drive.files().list()
                .setFields("files(id, name, webContentLink)")
                .execute();
        link = driveFiles.getFiles().stream()
                .filter(file -> fileName.equals(file.getName()))
                .map(com.google.api.services.drive.model.File::getWebContentLink)
                .findAny()
                .orElse("Something went wrong, the link is not available...");
        return link;
    }
}
