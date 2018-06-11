package com.spronq.mbt;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


/**
 * Hello world!
 *
 */
public class VdekMockAdaptor {

    public static void main(String[] args) throws IOException {
        int port = 4444;
        ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByAddress(new byte[]{0x7f, 0x00, 0x00, 0x01}));
        System.err.println("Started server on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.err.println("Accepted connection from client: " + clientSocket.getRemoteSocketAddress());

            In in = new In(clientSocket);
            Out out = new Out(clientSocket);

            String s;
            String ss;
            while ((s = in.readLine()) != null) {
                System.out.println(s);
                ss = processLine(s);
                System.out.println("ss:" + ss);
            }

            System.err.println("Closing connection with client: " + clientSocket.getInetAddress());
            out.close();
            in.close();
            clientSocket.close();
        }
    }

    private static String processLine(String line) {
        String[] tokens = line.split("[(,)]+");
        System.out.println(java.util.Arrays.toString(tokens));

        switch(tokens[0]) {
            case "GetUserByEmail_Req":
                break;
            case "GetUserById_Req":
                break;
            case "GetShipment_Req":
                getShipmentReq(tokens);
                break;
            default :
                getShipmentReq(tokens); //Just for testing purposes only....
                System.out.println("ERR - Unknown call: " + tokens[0]);
        }


        return "";
    }

    private static void getShipmentReq(String[] tokens) {
        String url = "http://localhost:8080/shipments/1";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = restTemplate.headForHeaders(url);

        ResponseEntity response = restTemplate.getForEntity(url, String.class);
        System.out.println(response);
    }
}