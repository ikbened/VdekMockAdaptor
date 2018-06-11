package com.spronq.mbt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class In {
    private Scanner scanner;

    public In(java.net.Socket socket) {
        try {
            InputStream is = socket.getInputStream();
            scanner = new Scanner(new BufferedInputStream(is), "UTF-8");
        } catch (IOException ioe) {
            System.err.println("Could not open " + socket);
        }
    }

    public String readLine() {
        String line;
        try {
            line = scanner.nextLine();
        } catch (Exception e) {
            line = null;
        }
        return line;
    }

    public void close() {
        scanner.close();
    }
}

