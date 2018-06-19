package com.spronq.mbt;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * For testing use netcat: nc -v 127.0.0.1 4444
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

        switch (tokens[0].toUpperCase()) {
            case "POSTUSER_REQ":
                response = PostUser(tokens);
                break;
            case "GETUSERBYEMAIL_REQ":
                response = GetUserByEmail(tokens);
                break;
            case "GETUSERBYID_REQ":
                response = GetUserById(tokens);
                break;
            case "POSTSHIPMENT_REQ":
                response = PostShipment(tokens);
                break;
            case "GETSHIPMENT_REQ":
                response = getShipment(tokens);
                break;
            default :
                System.out.println("ERR - Unknown call: " + tokens[0]);
        }

        return response;
    }

    private static String PostUser(String[] tokens) {
        RestTemplate restTemplate  = new RestTemplate();
        String response;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");

        JSONObject user = new JSONObject();
        user
            .put("email", tokens[1])
            .put("label", tokens[2])
            .put("customerNumber", tokens[3]);

        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(user.toString(), httpHeaders);
            String re = restTemplate.postForObject(baseUrl + "/users", httpEntity, String.class);
            response = makeUserOutput(new JSONObject(re));
            response = "PostUser_Resp(" + response + ")";
        } catch (HttpClientErrorException ex) {
            response = "PostUser_Resp_Err(" + ex.getRawStatusCode() + ")";
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


    private static String GetUserByEmail(String[] tokens) {
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

    private static String PostShipment(String[] tokens) {RestTemplate restTemplate  = new RestTemplate();
        String response;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");

        SimpleDateFormat yyyymmddhh = new SimpleDateFormat("yyyymmddhh");
        SimpleDateFormat mmss = new SimpleDateFormat("mmss");

        JSONObject extShipment = new JSONObject();
        extShipment
            .put("customerNumber", tokens[3])
            .put("ean", "9789034506801")
            .put("orderId", yyyymmddhh.format(new Date()))
            .put("orderLine", mmss.format(new Date()))
            .put("schoolId", "1641")
            .put("sessionId", "")
            .put("emailAddress", tokens[1])
            .put("label", "VDE")
            .put("postalCode", "2323ab")
            .put("firstName", "Bokito")
            .put("middleName", "de")
            .put("lastName", "Aap")
            .put("groupName", "")
            .put("administration", "Dynamics")
            .put("address", "SomeStreet")
            .put("addressNumber", "1")
            .put("addressAdjunct", "")
            .put("city", "SomeCity")
            .put("country", "SomeCountry")
            .put("gender", "M")
            .put("birthDate", "2004-02-01")
            .put("amount", "1")
            .put("startDate", "2018-04-27")
            .put("displayName", "SomeDisplayName")
            .put("emailUser", tokens[2]);



        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(extShipment.toString(), httpHeaders);
            String re = restTemplate.postForObject(baseUrl + "/shipments", httpEntity, String.class);
            response = makeShipmentOutput(new JSONObject(re));
            response = "PostShipment_Resp(" + response + ")";
        } catch (HttpClientErrorException ex) {
            response = "PostShipment_Resp_Err(" + ex.getRawStatusCode() + ")";
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
