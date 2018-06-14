package com.spronq.mbt;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * For testing use netcat (nc -v 127.0.0.1 4444)
 *
 */
public class VdekMockAdaptor {
    private static final String baseUrl = "http://localhost:8080";

    public static void main(String[] args) throws IOException {
        int port = 4444;
        ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByAddress(new byte[]{0x7f, 0x00, 0x00, 0x01}));
        System.err.println("Started server on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.err.println("Accepted connection from client: " + clientSocket.getRemoteSocketAddress());

            In in = new In(clientSocket);
            Out out = new Out(clientSocket);

            String lineIn;
            String lineOut;
            while ((lineIn = in.readLine()) != null) {
                System.out.println("in:  " + lineIn);
                lineOut = callApi(lineIn);
                out.println(lineOut);
                System.out.println("out: " + lineOut);
            }

            System.err.println("Closing connection with client: " + clientSocket.getInetAddress());
            out.close();
            in.close();
            clientSocket.close();
        }
    }

    private static String callApi(String line) {
        String response = "";
        String[] tokens = line.split("[(,)]+");

        switch(tokens[0]) {
            case "PostUser_Req":
                break;
            case "GetUsersByEmail_Req":
                response = GetUsersByEmail(tokens);
                break;
            case "GetUserById_Req":
                response = GetUserById(tokens);
                break;
            case "PostShipment_Req":
                break;
            case "GetShipment_Req":
                response = getShipment(tokens);
                break;
            default :
                System.out.println("ERR - Unknown call: " + tokens[0]);
        }

        return response;
    }


    private static String GetUserById(String[] tokens) {
        RestTemplate restTemplate;
        String response;

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + "/users/" + tokens[1]);
        String uriBuilder = builder.build().encode().toUriString();

        try {
            restTemplate = new RestTemplate();
            String re = restTemplate.getForObject(uriBuilder, String.class);
            response = makeUserOutput(new JSONObject(re));
            response = "GetUser_Resp(" + response + ")";
        } catch (HttpClientErrorException ex) {
            response = "GetUser_Resp_Err(" + ex.getRawStatusCode() + ")";
        }

        return response;
    }


    private static String GetUsersByEmail(String[] tokens) {
        RestTemplate restTemplate;
        String response = "";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + "/users")
                .queryParam("email", tokens[1]);
        String uriBuilder = builder.build().encode().toUriString();

        try {
            restTemplate = new RestTemplate();
            String re = restTemplate.getForObject(uriBuilder, String.class);

            JSONArray users = new JSONArray(re);
            for (int i = 0; i < users.length(); i++) {
                if (!StringUtils.isEmpty(response)) {
                    response = response + ",";
                }
                response = response + makeUserOutput(users.getJSONObject(i));
                System.out.println(response);
            }
            response = "GetUser_Resp(" + response + ")";
        } catch (HttpClientErrorException ex) {
            response = "GetUser_Resp_Err(" + ex.getRawStatusCode() + ")";
        }

        return response;
    }


    private static String getShipment(String[] tokens) {
        String url = baseUrl + "/shipments/" + tokens[1];
        RestTemplate restTemplate;
        String response;

        try {
            restTemplate = new RestTemplate();
            ResponseEntity re = restTemplate.getForEntity(url, String.class);
            response = makeShipmentOutput(new JSONObject(re.getBody().toString()));
            response = "GetShipment_Resp(" + response + ")";
        } catch (HttpClientErrorException ex) {
            response = "GetShipment_Resp_Err(" + ex.getRawStatusCode() + ")";
        }

        return response;
    }

    private static String makeShipmentOutput(JSONObject shipment) {
        List<String> attributes =  Arrays.asList("shipmentId","emailAddress","emailUser","processedByTask","errorMessage");
        String response = "";

        for(String attribute:attributes){
            if(!StringUtils.isEmpty(response)) {
                response = response + ",";
            }

            if (shipment.has(attribute)) {
                response = response + shipment.get(attribute);
            }
        }
        return response;
    }

    private static String makeUserOutput(JSONObject user) {
        String response = "";

        List<String> attributes =  Arrays.asList("id", "label", "email", "customerNumber", "accountSetId", "postalCode");
        for(String attribute:attributes){
            if(!StringUtils.isEmpty(response)) {
                response = response + ",";
            }

            if (user.has(attribute)) {
                response = response + user.get(attribute);
            }
        }
        return "User(" + response + ")";
    }
}
