package com.aichecker.service.impl;

import com.aichecker.exception.ApiException;
import com.aichecker.service.OcrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class OcrServiceImpl implements OcrService {

    @Value("${app.ocr.tesseract-enabled:true}")
    private boolean tesseractEnabled;

    @Override
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        if (!tesseractEnabled) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OCR is disabled. Enable app.ocr.tesseract-enabled");
        }

        String original = file.getOriginalFilename() == null ? "upload.png" : file.getOriginalFilename();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".png";
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("ocr-upload-", extension);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            ProcessBuilder processBuilder = new ProcessBuilder("tesseract", tempFile.toString(), "stdout", "-l", "eng");
            Process process = processBuilder.start();

            String stdout;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                stdout = sb.toString();
            }

            String stderr;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                stderr = sb.toString();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "OCR failed. Ensure Tesseract is installed. Details: " + stderr.trim());
            }

            return stdout.trim();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to run OCR. Make sure Tesseract is installed and available in PATH");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR process interrupted");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // No-op cleanup failure.
                }
            }
        }
    }
}
