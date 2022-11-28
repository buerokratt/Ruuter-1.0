package rig.ruuter.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static rig.ruuter.constant.Constant.LINE_BREAK;

@Slf4j
public class FileUtils {

    private FileUtils() {
    }

    /**
     * Writes content to a file, makes directories
     *
     * @param parentDir String
     * @param filename  String
     * @param content   String
     */
    public static void writeFile(String parentDir, String filename,
                                 String content) {
        if (content == null || content.isEmpty() || content
                .equals(LINE_BREAK)) {
            return;
        }

        File parent = new File(parentDir);
        parent.mkdirs();
        final File f = new File(parent, filename);

        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(f),
                        StandardCharsets.UTF_8))) {
            writer.write(content);
        } catch (IOException e) {
            log.error("IOException trying to write a file.", e);
        }

    }

    public static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, Charset.forName("UTF-8"));
    }

}
