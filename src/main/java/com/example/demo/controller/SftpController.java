package com.example.demo.controller;

import com.example.demo.service.SftpService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Paths;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sftp")
public class SftpController {

   @Value("${sftp.remote-directory}")
   private String remoteDirectory;

    @Autowired
    private SftpService sftpService;

    @GetMapping("/ls")
    public ResponseEntity<?> listFiles(@RequestParam String directory) {
        return ResponseEntity.ok(sftpService.listFiles(directory));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getFile(@RequestParam("remoteFilePath") String remoteFilePath) {
        try {
            String message = sftpService.getFile(remoteFilePath);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/put")
      public ResponseEntity<?> putFile(@RequestParam("localFilePath") String localFilePath,
                                       @RequestParam("remoteDir") String remoteDir) {
         try {
            // Validate input parameters
            if (localFilePath == null || localFilePath.isEmpty()) {
                  return ResponseEntity.badRequest().body("Local file path is required.");
            }
            if (remoteDir == null || remoteDir.isEmpty()) {
                  return ResponseEntity.badRequest().body("Remote directory is required.");
            }

            String message = sftpService.putFile(localFilePath, remoteDir);
            return ResponseEntity.ok(message);
         } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(e.getMessage());
         } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
         }
      }

    @GetMapping("/mget")
    public ResponseEntity<?> mgetFiles(@RequestParam String remoteFilePathPattern) {
        return ResponseEntity.ok(sftpService.mgetFiles(remoteFilePathPattern));
    }

    @PostMapping("/mput")
    public ResponseEntity<?> putFiles(@RequestParam("localFilePathPattern") String localFilePathPattern,
                                      @RequestParam("remoteDir") String remoteDir) {
        try {
            // Validate input parameters
            if (localFilePathPattern == null || localFilePathPattern.isEmpty()) {
                return ResponseEntity.badRequest().body("Local file path pattern is required.");
            }
            if (remoteDir == null || remoteDir.isEmpty()) {
                return ResponseEntity.badRequest().body("Remote directory is required.");
            }

            String message = sftpService.putFiles(localFilePathPattern, remoteDir);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
