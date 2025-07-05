package dev.loadless.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final String LOGS_DIR = "logs";
    private static final String LATEST_LOG = "latest.log";
    private final PrintWriter writer;
    private final PrintWriter latestWriter;

    public Logger() throws IOException {
        Files.createDirectories(Path.of(LOGS_DIR));
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File logFile = new File(LOGS_DIR, timestamp + ".log");
        File latestFile = new File(LOGS_DIR, LATEST_LOG);
        this.writer = new PrintWriter(new FileWriter(logFile, true), true);
        this.latestWriter = new PrintWriter(new FileWriter(latestFile, false), true);
    }

    public void log(String msg) {
        String line = timePrefix() + msg;
        System.out.println(line);
        writer.println(line);
        latestWriter.println(line);
    }

    public void error(String msg) {
        String line = timePrefix() + "[ERROR] " + msg;
        System.err.println(line);
        writer.println(line);
        latestWriter.println(line);
    }

    private String timePrefix() {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
    }

    public void close() {
        writer.close();
        latestWriter.close();
    }
}
