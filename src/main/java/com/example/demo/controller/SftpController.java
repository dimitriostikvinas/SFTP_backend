package com.example.demo.controller;

import com.example.demo.service.SftpService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sftp")
public class SftpController {

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
            String message = sftpService.putFile(localFilePath, remoteDir);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/mget")
    public ResponseEntity<?> mgetFiles(@RequestParam String remoteFilePathPattern) {
        return ResponseEntity.ok(sftpService.mgetFiles(remoteFilePathPattern));
    }

    @PostMapping("/mput")
    public ResponseEntity<?> mputFiles(@RequestParam String localFilePathPattern, @RequestParam String remoteDir) {
        return ResponseEntity.ok(sftpService.mputFiles(localFilePathPattern, remoteDir));
    }
}
