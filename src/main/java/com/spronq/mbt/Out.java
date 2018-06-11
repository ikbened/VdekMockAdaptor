package com.spronq.mbt;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Out {
    private PrintWriter out;

    public Out(Socket socket) {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void close() {
        out.close();
    }

    public void println(Object x) {
        out.println(x);
        out.flush();
    }
}


