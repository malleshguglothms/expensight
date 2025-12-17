package com.gm.expensight.config;

import com.sun.jna.NativeLibrary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class TesseractLibraryInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static {
        configureTesseractLibraryPath();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Configuration already done in static block
    }

    private static void configureTesseractLibraryPath() {
        String libraryPath = findTesseractLibraryPath();
        if (libraryPath != null) {
            try {
                NativeLibrary.addSearchPath("tesseract", libraryPath);
                System.setProperty("jna.library.path", libraryPath);
                log.info("Configured Tesseract library path: {}", libraryPath);
            } catch (Exception e) {
                log.warn("Failed to configure library path: {}", e.getMessage());
            }
        } else {
            log.warn("Tesseract library not found. Set DYLD_LIBRARY_PATH=/opt/homebrew/lib if needed.");
        }
    }

    private static String findTesseractLibraryPath() {
        String[] possiblePaths = {
            "/opt/homebrew/lib",
            "/usr/local/lib",
            "/usr/lib"
        };

        for (String pathStr : possiblePaths) {
            Path libPath = Paths.get(pathStr, "libtesseract.dylib");
            if (Files.exists(libPath)) {
                return pathStr;
            }
        }

        return null;
    }
}

