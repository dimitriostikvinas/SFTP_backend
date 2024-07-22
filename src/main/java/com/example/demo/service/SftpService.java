package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.io.File;

@Service
public class SftpService {

    @Autowired
    private MessagingTemplate messagingTemplate;

    @Autowired
    private MessageChannel sftpLsChannel;

    @Autowired
    private MessageChannel sftpGetChannel;

    @Autowired
    private MessageChannel sftpPutChannel;

    @Autowired
    private MessageChannel sftpMgetChannel;

    @Autowired
    private MessageChannel sftpMputChannel;

    @Value("${sftp.remote-directory}")
    private String remoteDirectory;

    @Value("${sftp.local-directory}")
    private String localDirectory;



    public List<FileInfo> listFiles(String directory) {
        if (directory == null || directory.isEmpty()) {
            directory = remoteDirectory;
        }

        Message<String> message = MessageBuilder.withPayload(directory).build();
        Message<?> result = messagingTemplate.sendAndReceive(sftpLsChannel, message);

        if (result == null) {
            throw new RuntimeException("Failed to list files from SFTP server");
        }

        @SuppressWarnings("unchecked")
        List<SftpFileInfo> payload = (List<SftpFileInfo>) result.getPayload();
        List<FileInfo> fileInfoList = new ArrayList<>();
        for (SftpFileInfo entry : payload) {
            FileInfo fileInfo = new FileInfo(
                entry.getFilename(),
                entry.isDirectory() ? "directory" : "file",
                entry.getSize(),
                entry.getModified() * 1000L
            );
            fileInfoList.add(fileInfo);
        }

        return fileInfoList;
    }

    public static class FileInfo {
        private String filename;
        private String filetype;
        private long size;
        private long lastModified;

        public FileInfo(String filename, String filetype, long size, long lastModified) {
            this.filename = filename;
            this.filetype = filetype;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getFilename() {
            return filename;
        }

        public String getFiletype() {
            return filetype;
        }

        public long getSize() {
            return size;
        }

        public long getLastModified() {
            return lastModified;
        }
    }

    public String getFile(String remoteFilePath) {
        Message<String> message = MessageBuilder.withPayload(remoteFilePath).build();
        Message<?> result = messagingTemplate.sendAndReceive(sftpGetChannel, message);

        if (result == null) {
            throw new RuntimeException("Failed to retrieve file from SFTP server");
        }

        File localFile = Paths.get(localDirectory, new File(remoteFilePath).getName()).toFile();
        if (!localFile.exists()) {
            throw new RuntimeException("Failed to save file to local directory, it already exists");
        }

        return "File downloaded and stored in local directory: " + localFile.getAbsolutePath();
    }

    public String putFile(String relativeLocalFilePath, String remoteDir) {
        try {
            // Convert the relative path to an absolute path based on the project's base directory
            File localFile = Paths.get(localDirectory, relativeLocalFilePath).toFile();
            if (!localFile.exists()) {
                throw new RuntimeException("Local file does not exist: " + localFile.getAbsolutePath());
            }

            String payload = "file:" + localFile.getAbsolutePath() + ">" + remoteDir;
            Message<String> message = MessageBuilder.withPayload(payload).build();
            Message<?> result = messagingTemplate.sendAndReceive(sftpPutChannel, message);

            if (result == null) {
                throw new RuntimeException("Failed to put file to SFTP server");
            }

            return "File uploaded to SFTP server: " + remoteDir + "/" + localFile.getName();
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            throw new RuntimeException("Error occurred while putting file to SFTP server", e);
        }
    }

    public Object mgetFiles(String remoteFilePathPattern) {
        return messagingTemplate.sendAndReceive(sftpMgetChannel, MessageBuilder.withPayload(remoteFilePathPattern).build());
    }

    public Object mputFiles(String localFilePathPattern, String remoteDir) {
        String payload = "file:" + localFilePathPattern + ">" + remoteDir;
        return messagingTemplate.sendAndReceive(sftpMputChannel, MessageBuilder.withPayload(payload).build());
    }
}
