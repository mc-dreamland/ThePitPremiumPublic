package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

final class ScanReportPath {

    private ScanReportPath() {
    }

    static Path directory(String scanType) throws IOException {
        Path directory = ThePit.getInstance().getDataFolder().toPath()
                .resolve("logs")
                .resolve("scan")
                .resolve(sanitize(scanType));
        Files.createDirectories(directory);
        return directory;
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return normalized.isBlank() ? "unknown" : normalized;
    }
}
