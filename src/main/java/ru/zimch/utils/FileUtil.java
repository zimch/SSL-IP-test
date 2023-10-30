package ru.zimch.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static void writeDomainToFile(String domainInfo) {
        domainInfo = domainInfo + "\n";

        try (FileOutputStream fos = new FileOutputStream("domainLog.txt", true)) {
            byte[] buffer = domainInfo.getBytes();

            fos.write(buffer, 0, buffer.length);
            logger.debug("[File]: The file has been written");
        } catch(IOException e){
            logger.error("[IOException]: " + e.getMessage());
        }
    }

    public static String getFileContent(String fileName) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/" + fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }
}
