package com.sankhya.dataset.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(@Value("${sankhya.storage.upload-dir:./data/uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload storage folder", e);
        }
    }

    public String storeFile(Long workspaceId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        String storedFileName = UUID.randomUUID() + extension;
        Path workspaceFolder = this.rootLocation.resolve("workspaces").resolve(String.valueOf(workspaceId)).normalize();
        Files.createDirectories(workspaceFolder);

        Path destinationFile = workspaceFolder.resolve(storedFileName).normalize();
        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside current directory boundary.");
        }

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return destinationFile.toString();
    }

    public File getFile(String filePath) throws FileNotFoundException {
        Path path = Paths.get(filePath).normalize();
        if (!path.startsWith(this.rootLocation)) {
            throw new SecurityException("Access to path outside storage directory is denied: " + filePath);
        }
        File file = path.toFile();
        if (!file.exists()) {
            throw new FileNotFoundException("File not found at path: " + filePath);
        }
        return file;
    }

    public InputStream getInputStream(String filePath) throws IOException {
        File file = getFile(filePath);
        return new FileInputStream(file);
    }

    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            if (path.startsWith(this.rootLocation)) {
                return Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            // log error
        }
        return false;
    }
}
