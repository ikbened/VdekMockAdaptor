package com.spronq.mbt;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
 * Examples (request + response(s)):
 * - PostUser_Req(aap@aap.nl,learnId,1234AB)
 *      PostUser_Resp(User(5b7931797ae2185d01fb7470,learnId,aap@aap.nl,1234AB))
 *      PostUser_Resp_Err(400)
 * - GetUserByEmail_Req(aap@aap.nl)
 *      GetUser_Resp(User(5b7931797ae2185d01fb7470,learnId,aap@aap.nl,1234AB),
 *      GetUser_Resp(User(5b7931797ae2185d01fb7470,learnId,aap@aap.nl,1234AB),User(5b7932197ae2185d01fb7471,learnId,aap@aap.nl,1234CD))
 *      GetUser_Resp_Err()
 * - GetUserById_Req(5b7931797ae2185d01fb7470)
 *      GetUser_Resp(User(5b7931797ae2185d01fb7470,learnId,aap@aap.nl,1234AB))
 *      GetUser_Resp_Err(404)
 * - PostUserClaim_Req(5b7933b27ae2185d01fb7472,customerNumber,1819)
 *      PostUserClaim_Resp(UserClaim(5b793d4d7ae2185d01fb7475,5b7933b27ae2185d01fb7472,customerNumber,1819))
 * - GetUserClaimsByUserId_req(5b7933b27ae2185d01fb7472)
 *      GetUserClaims_Resp(UserClaim(5b7934007ae2185d01fb7473,5b7933b27ae2185d01fb7472,customerNumber,1819),UserClaim(5b79363d7ae2185d01fb7474,5b7933b27ae2185d01fb7472,customerNumber,1920),UserClaim(5b793d4d7ae2185d01fb7475,5b7933b27ae2185d01fb7472,customerNumber,2021))
 * - PostShipment_req(cust@mailinator.com,user@mailinator.com,18)
 *      PostShipment_Resp(5b793f7a7ae2185d01fb7476,cust@mailinator.com,user@mailinator.com,true,)
 *      PostShipment_Resp_Err(400)
 *      PostShipment_Resp(5b793fe67ae2185d01fb7477,aap@aap.nl,user@mailinator.com,false,Customer email is not unique within LearnId)
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

    private static String checkInput(String line) {
        if (line.length() == 0) {
            return "No input";
        } else if (!line.contains("(") || !line.contains(")")) {
            return "Missing parentheses";
        } else if (line.indexOf("(") == 0 ) {
            return "Missing keyword";
        } else {
            return "";
        }
    }

    private static List<String> mySplit(String line) {
        List<String> tokens = new ArrayList<>();

        tokens.add(line.substring(0, line.indexOf("(")));

        String s = line.substring(line.indexOf("(")+1, line.lastIndexOf(")"));
        int i;
        int j=-1;
        for (i=0; i<s.length(); i++) {
            if (s.substring(i, i+1).equalsIgnoreCase(",")) {
                tokens.add(s.substring(j+1, i));
                j=i;
            }

        }
        tokens.add(s.substring(j+1, i));

        return tokens;
    }

    private static String callApi(String line) {
        String response;
        List<String> tokens;

        response = checkInput(line);
        if (response.length() > 0) {
            return response;
        } else {
            tokens = mySplit(line);
        }

        switch (tokens.get(0).toUpperCase()) {
            case "POSTUSER_REQ":
                response = PostUser(tokens);
                break;
            case "GETUSERBYEMAIL_REQ":
                response = GetUsersByEmail(tokens);
                break;
            case "GETUSERBYID_REQ":
                response = GetUserById(tokens);
                break;
            case "POSTSHIPMENT_REQ":
                response = PostShipment(tokens);
                break;
            case "GETSHIPMENTBYID_REQ":
                response = getShipmentById(tokens);
                break;
            case "POSTUSERCLAIM_REQ":
                response = postUserClaim(tokens);
                break;
            case "GETUSERCLAIMSBYUSERID_REQ":
                response = getUserClaimsByUserId(tokens);
                break;
            default :
                System.out.println("ERR - Unknown call: " + tokens.get(0));
                response = "Unknown keyword";
        }

        return response;
    }

    private static String PostUser(List<String> tokens) {
        String response;

        if (tokens.size() != 4){
            response = "PostUser_Resp_Err(400)";
        } else {
            RestTemplate restTemplate  = new RestTemplate();
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Content-Type", "application/json");

            JSONObject user = new JSONObject();
            user
                    .put("email", tokens.get(1))
                    .put("label", tokens.get(2))
                    .put("postalCode", tokens.get(3));

            try {
                HttpEntity<String> httpEntity = new HttpEntity<>(user.toString(), httpHeaders);
                String re = restTemplate.postForObject(baseUrl + "/users", httpEntity, String.class);
                response = makeUserOutput(new JSONObject(re));
                response = "PostUser_Resp(" + response + ")";
            } catch (HttpClientErrorException ex) {
                response = "PostUser_Resp_Err(" + ex.getRawStatusCode() + ")";
            }

        }

        return response;
    }


    private static String GetUserById(List<String> tokens) {
        RestTemplate restTemplate;
        String response;

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + "/users/" + tokens.get(1));
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


    private static String GetUsersByEmail(List<String> tokens) {
        RestTemplate restTemplate;
        String response = "";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + "/users")
                .queryParam("email", tokens.get(1));
        String uriBuilder = builder.build().encode().toUriString();

        try {
            restTemplate = new RestTemplate();
            String re = restTemplate.getForObject(uriBuilder, String.class);

            JSONArray users = new JSONArray(re);
            if (users.length() == 0) {
                response = "GetUser_Resp_Err(404)";
            } else {
                for (int i = 0; i < users.length(); i++) {
                    if (!StringUtils.isEmpty(response)) {
                        response = response + ",";
                    }
                    response = response + makeUserOutput(users.getJSONObject(i));
                    System.out.println(response);
                }
                response = "GetUser_Resp(" + response + ")";
            }
        } catch (HttpClientErrorException ex) {
            response = "GetUser_Resp_Err(" + ex.getRawStatusCode() + ")";
        }

        return response;
    }


    private static String getShipmentById(List<String> tokens) {
        String url = baseUrl + "/shipments/" + tokens.get(1);
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

    private static String PostShipment(List<String> tokens) {
        RestTemplate restTemplate  = new RestTemplate();
        String response;

        if (tokens.size() != 4) {
            response = "PostShipment_Resp_Err(400)";
        } else {

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Content-Type", "application/json");

            SimpleDateFormat yyyymmddhh = new SimpleDateFormat("yyyymmddhh");
            SimpleDateFormat mmss = new SimpleDateFormat("mmss");

            JSONObject extShipment = new JSONObject();
            extShipment
                    .put("customerNumber", tokens.get(3))
                    .put("ean", "9789034506801")
                    .put("orderId", yyyymmddhh.format(new Date()))
                    .put("orderLine", mmss.format(new Date()))
                    .put("schoolId", "1641")
                    .put("sessionId", "")
                    .put("emailAddress", tokens.get(1))
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
                    .put("emailUser", tokens.get(2));

            try {
                HttpEntity<String> httpEntity = new HttpEntity<>(extShipment.toString(), httpHeaders);
                String re = restTemplate.postForObject(baseUrl + "/shipments", httpEntity, String.class);
                response = makeShipmentOutput(new JSONObject(re));
                response = "PostShipment_Resp(" + response + ")";
            } catch (HttpClientErrorException ex) {
                response = "PostShipment_Resp_Err(" + ex.getRawStatusCode() + ")";
            }
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

        List<String> attributes =  Arrays.asList("id", "label", "email", "postalCode");
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

    private static String makeUserClaimOutput(JSONObject userClaim) {
        String response = "";

        List<String> attributes =  Arrays.asList("id", "userId", "claimType", "claimValue");
        for(String attribute:attributes){
            if(!StringUtils.isEmpty(response)) {
                response = response + ",";
            }

            if (userClaim.has(attribute)) {
                response = response + userClaim.get(attribute);
            }
        }
        return "UserClaim(" + response + ")";
    }

    private static String getUserClaimsByUserId(List<String> tokens) {
        RestTemplate restTemplate;
        String response = "";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + "/userclaims")
                .queryParam("userId", tokens.get(1));
        String uriBuilder = builder.build().encode().toUriString();

        try {
            restTemplate = new RestTemplate();
            String re = restTemplate.getForObject(uriBuilder, String.class);

            JSONArray uc = new JSONArray(re);

            if(uc.length() == 0) {
                response = "GetUserClaims_Resp_Err(404)";
            } else {
                for (int i = 0; i < uc.length(); i++) {
                    if (!StringUtils.isEmpty(response)) {
                        response = response + ",";
                    }
                    response = response + makeUserClaimOutput(uc.getJSONObject(i));
                    System.out.println(response);
                }
                response = "GetUserClaims_Resp(" + response + ")";
            }
        } catch (HttpClientErrorException ex) {
            response = "GetUserClaims_Resp_Err(" + ex.getRawStatusCode() + ")";
        }

        return response;

    }

    private static String postUserClaim(List<String> tokens) {
        RestTemplate restTemplate  = new RestTemplate();
        String response;

        if (tokens.size() != 4) {
            response = "PostShipment_Resp_Err(400)";
        } else {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Content-Type", "application/json");

            JSONObject uc = new JSONObject();
            uc
                    .put("userId", tokens.get(1))
                    .put("claimType", tokens.get(2))
                    .put("claimValue", tokens.get(3));

            try {
                HttpEntity<String> httpEntity = new HttpEntity<>(uc.toString(), httpHeaders);
                String re = restTemplate.postForObject(baseUrl + "/userclaims", httpEntity, String.class);
                response = makeUserClaimOutput(new JSONObject(re));
                response = "PostUserClaim_Resp(" + response + ")";
            } catch (HttpClientErrorException ex) {
                response = "PostUserClaim_Resp_Err(" + ex.getRawStatusCode() + ")";
            }
        }
        return response;
    }
}
