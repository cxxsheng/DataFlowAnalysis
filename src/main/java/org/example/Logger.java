package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Logger {
    private static final String LOG_FILE_PATH = "log.txt";

    public static void log(String message) {
        String formattedMessage = getFormattedLogMessage(message);
        System.out.println(message);
        writeToFile(formattedMessage);
    }

    private static String getFormattedLogMessage(String message) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String formattedDateTime = now.format(formatter);
        return "[" + formattedDateTime + "] " + message;
    }

    private static void writeToFile(String message) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true));
            writer.write(message);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}