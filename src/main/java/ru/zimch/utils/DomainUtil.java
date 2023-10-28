package ru.zimch.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileOutputStream;
import java.io.IOException;

public class DomainUtil {

    private static final Logger logger = LoggerFactory.getLogger(DomainUtil.class);

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
}
