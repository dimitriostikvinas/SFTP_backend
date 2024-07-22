package com.example.demo.service;

import com.example.demo.model.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SftpService {

    private static final Logger logger = LoggerFactory.getLogger(SftpService.class);

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

    @Autowired
    private MessageChannel sftpReplyChannel;

    @Value("${sftp.remote-directory}")
    private String remoteDirectory;

    @Value("${sftp.local-directory}")
    private String localDirectory;

    public List<FileInfo> listFiles(String directory) {
        if (directory == null || directory.isEmpty()) {
            directory = remoteDirectory;
        }

        logger.info("Listing files in directory: {}", directory);
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

    public String getFile(String remoteFilePath) {
        logger.info("Retrieving file from SFTP server: {}", remoteFilePath);
        Message<String> message = MessageBuilder.withPayload(remoteFilePath).build();
        Message<?> result = messagingTemplate.sendAndReceive(sftpGetChannel, message);

        if (result == null) {
            throw new RuntimeException("Failed to retrieve file from SFTP server");
        }

        File localFile = Paths.get(localDirectory, new File(remoteFilePath).getName()).toFile();
        if (localFile.exists()) {
            logger.info("File downloaded and stored in local directory: {}", localFile.getAbsolutePath());
            return "File downloaded and stored in local directory: " + localFile.getAbsolutePath();
        } else {
            throw new RuntimeException("Failed to save file to local directory.");
        }
    }

    public String putFile(String relativeLocalFilePath, String remoteDir) {
        try {
            File localFile = Paths.get(localDirectory, relativeLocalFilePath).toFile();
            if (!localFile.exists()) {
                throw new RuntimeException("Local file does not exist: " + localFile.getAbsolutePath());
            }

            logger.info("Uploading file to SFTP server: {}", localFile.getAbsolutePath());
            Message<File> message = MessageBuilder.withPayload(localFile)
                                                  .setHeader("remoteDir", remoteDir)
                                                  .setReplyChannel(sftpReplyChannel)
                                                  .build();

            Message<?> result = messagingTemplate.sendAndReceive(sftpPutChannel, message);

            if (result == null) {
                throw new RuntimeException("Failed to put file to SFTP server");
            }

            logger.info("File uploaded to SFTP server: {}/{}", remoteDir, localFile.getName());
            return "File uploaded to SFTP server: " + remoteDir + "/" + localFile.getName();
        } catch (Exception e) {
            logger.error("Error occurred while putting file to SFTP server", e);
            throw new RuntimeException("Error occurred while putting file to SFTP server", e);
        }
    }

    public List<String> mgetFiles(String remoteFilePathPattern) {
        logger.info("Retrieving files from SFTP server with pattern: {}", remoteFilePathPattern);
        Message<String> message = MessageBuilder.withPayload(remoteFilePathPattern).build();
        Message<?> result = messagingTemplate.sendAndReceive(sftpMgetChannel, message);

        if (result == null) {
            throw new RuntimeException("Failed to retrieve files from SFTP server");
        }

        @SuppressWarnings("unchecked")
        List<String> localFilePaths = (List<String>) result.getPayload();
        logger.info("Files retrieved: {}", localFilePaths);
        return localFilePaths;
    }

    public String putFiles(String localFilePathPattern, String remoteDir) {
        try {
            // Ensure local directory path is correct
            Path localDirPath = Paths.get(localDirectory);
            if (!Files.exists(localDirPath)) {
                throw new RuntimeException("Local directory does not exist: " + localDirPath.toAbsolutePath());
            }

            // Log local directory path and file pattern
            logger.info("Uploading files from local directory: {} with pattern: {}", localDirPath, localFilePathPattern);

            // List all files in local directory for debugging
            Files.list(localDirPath).forEach(path -> logger.info("File in local directory: {}", path));

            // Correct the pattern to match *.csv files
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + localFilePathPattern);
            List<File> matchingFiles = Files.walk(localDirPath)
                                            .filter(path -> matcher.matches(path.getFileName()))
                                            .map(Path::toFile)
                                            .collect(Collectors.toList());

            // Log matching files found
            if (matchingFiles.isEmpty()) {
                throw new RuntimeException("No local files match the provided pattern: " + localFilePathPattern);
            } else {
                logger.info("Found matching files: {}", matchingFiles);
            }

            // Upload each matching file
            for (File localFile : matchingFiles) {
                Message<File> message = MessageBuilder.withPayload(localFile)
                                                      .setHeader("remoteDir", remoteDir)
                                                      .setReplyChannel(sftpReplyChannel)
                                                      .build();
                messagingTemplate.send(sftpMputChannel, message);
            }

            logger.info("Files uploaded to SFTP server: {}", remoteDir);
            return "Files uploaded to SFTP server: " + remoteDir;
        } catch (Exception e) {
            logger.error("Error occurred while putting files to SFTP server", e);
            throw new RuntimeException("Error occurred while putting files to SFTP server", e);
        }
    }
}
