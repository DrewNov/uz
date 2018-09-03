package main;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class UzApi {
    private static boolean debugMode = false;

    private static HttpClient httpclient = HttpClients.createDefault();
    private static Header[] headers = new Header[4];

    public static String MAIN_URL = "https://booking.uz.gov.ua";
    private static String TRAINS_URL = MAIN_URL + "/train_search/";
    private static String COACHES_URL = MAIN_URL + "/purchase/coaches/";
    private static String COACH_URL = MAIN_URL + "/purchase/coach/";
    private static String CART_URL = MAIN_URL + "/cart/add/";

    public static void main() throws IOException {
        int repeats = 0;
        int maxRepeats = 3;

        //sendEmail(); todo

        initHeaders();

        // Temporary setting exact token and session-id
//        httpclient = HttpClients.createDefault();
//        headers[0] = new BasicHeader("GV-Ajax", "1");
//        headers[1] = new BasicHeader("GV-Referer", MAIN_URL);
//        headers[2] = new BasicHeader("GV-Token", "31d97b53d632880fb64da1aa402a8dfd");
//        headers[3] = new BasicHeader("Cookie", "_gv_sessid=n4qsg8iinfbef2rhtmvu0ldds3; path=/");
        System.out.println("Init Headers:\n" + Arrays.toString(headers) + "\n");

        // Executing requests
        try {
            JSONObject trains = getTrains("2210800", "2200001", "25.09.2016");
            System.out.println("Answer:\n" + trains.toString(4));

            JSONObject coach = getCoach("2210800", "2200001", "072П", "14", "25.09.2016 19:05");
            System.out.println(coach);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        for (int i = 1; i < Integer.MAX_VALUE; i++) {
//            String depDate1 = 25 + ".09.2016 19:05";
//            String depDate2 = 25 + ".09.2016 23:20";
//
//            System.out.println(i + ":");
//
//            try {
//                System.out.println("  " + depDate1);
//                printCouches(getCoaches("2210800", "2200001", "072П", "К", depDate1));
//                printCouches(getCoaches("2210800", "2200001", "072П", "П", depDate1));
//
//                System.out.println("  " + depDate2);
//                printCouches(getCoaches("2210800", "2200001", "012П", "К", depDate2));
//                printCouches(getCoaches("2210800", "2200001", "012П", "П", depDate2));
//
//                repeats = 0;
//            } catch (ClientProtocolException | NullPointerException e) {
//                initHeaders();
//                System.out.println("Session expired! New headers:\n" + Arrays.toString(headers));
//                if (repeats < maxRepeats) {
//                    i--;
//                    repeats++;
//                } else {
//                    repeats = 0;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            Thread.sleep(10000);
//        }
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;

        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString("UTF-8");
    }

    private static Header[] initHeaders() throws IOException {
        // Request to Main Page
        httpclient = HttpClients.createDefault();
        HttpResponse response = httpclient.execute(new HttpGet(MAIN_URL));

        // Decrypting token
        String body = inputStreamToString(response.getEntity().getContent());

        Map<String, String> keyMap = new HashMap<>(16);
        keyMap.put("___", "0");
        keyMap.put("__$", "1");
        keyMap.put("_$_", "2");
        keyMap.put("_$$", "3");
        keyMap.put("$__", "4");
        keyMap.put("$_$", "5");
        keyMap.put("$$_", "6");
        keyMap.put("$$$", "7");
        keyMap.put("$___", "8");
        keyMap.put("$__$", "9");
        keyMap.put("$_$_", "a");
        keyMap.put("$_$$", "b");
        keyMap.put("$$__", "c");
        keyMap.put("$$_$", "d");
        keyMap.put("$$$_", "e");
        keyMap.put("$$$$", "f");

        int a = body.indexOf("\"\\\\\\\"\"+") + 7; //7 is length of searching string
        int b = body.indexOf("+\"\\\\\\\");");

        String encrypted = body.substring(a, b).replace("$$_.", "");
        String decrypted = "";

        for (String s : encrypted.split("\\+")) {
            decrypted += keyMap.get(s);
        }

        // Setting Headers
        headers[0] = new BasicHeader("GV-Ajax", "1");
        headers[1] = new BasicHeader("GV-Referer", MAIN_URL);
        headers[2] = new BasicHeader("GV-Token", decrypted);
        headers[3] = new BasicHeader("Cookie", response.getFirstHeader("Set-Cookie").getValue());

        return headers;
    }

    public static JSONObject getTrains(String from, String to, String date) throws Exception {
        // Setting request header and parameters
        HttpPost httpPOST = new HttpPost(TRAINS_URL);

        //httpPOST.setHeaders(headers);
        //httpPOST.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        List<NameValuePair> params = new ArrayList<>(4);
        params.add(new BasicNameValuePair("from", from));
        params.add(new BasicNameValuePair("to", to));
        params.add(new BasicNameValuePair("date", date));
        params.add(new BasicNameValuePair("time", "00:00"));

        httpPOST.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        // Execute and get the response
        HttpResponse response = httpclient.execute(httpPOST);
        if (debugMode) {
            System.out.println(response.getStatusLine() + "\n");
        }

        return new JSONObject(inputStreamToString(response.getEntity().getContent())).getJSONObject("data");
    }

    private static JSONObject getCoaches(String from, String till, String train, String type, String depDate) throws Exception {
        // Setting request header and parameters
        HttpPost httpPOST = new HttpPost(COACHES_URL);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));

        httpPOST.setHeaders(headers);

        List<NameValuePair> params = new ArrayList<>(4);
        params.add(new BasicNameValuePair("station_id_from", from));
        params.add(new BasicNameValuePair("station_id_till", till));
        params.add(new BasicNameValuePair("train", train));
        params.add(new BasicNameValuePair("coach_type", type));
        params.add(new BasicNameValuePair("model", "0"));
        String ms = String.valueOf(sdf.parse(depDate).getTime());
        params.add(new BasicNameValuePair("date_dep", ms.substring(0, ms.length() - 3)));
        params.add(new BasicNameValuePair("round_trip", "0"));
        params.add(new BasicNameValuePair("another_ec", "0"));

        httpPOST.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        // Execute and get the response
        HttpResponse response = httpclient.execute(httpPOST);
        if (debugMode) {
            System.out.println(response.getStatusLine());
        }

        String responseStr = inputStreamToString(response.getEntity().getContent());
        JSONObject responseJson = null;
        try {
            responseJson = new JSONObject(responseStr);
        } catch (JSONException e) {
            System.out.println(responseStr);
            e.printStackTrace();
        }
        return responseJson;
    }

    private static JSONObject getCoach(String from, String till, String train, String coach, String depDate) throws Exception {
        // Setting request header and parameters
        HttpPost httpPOST = new HttpPost(COACH_URL);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));

        httpPOST.setHeaders(headers);

        List<NameValuePair> params = new ArrayList<>(4);
        params.add(new BasicNameValuePair("station_id_from", from));
        params.add(new BasicNameValuePair("station_id_till", till));
        params.add(new BasicNameValuePair("train", train));
        params.add(new BasicNameValuePair("coach_num", coach));
        params.add(new BasicNameValuePair("coach_class", "Б"));
        params.add(new BasicNameValuePair("coach_type_id", "3"));
        String ms = String.valueOf(sdf.parse(depDate).getTime());
        params.add(new BasicNameValuePair("date_dep", ms.substring(0, ms.length() - 3)));
        params.add(new BasicNameValuePair("change_scheme", "0"));

        httpPOST.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        // Execute and get the response
        HttpResponse response = httpclient.execute(httpPOST);
        if (debugMode) {
            System.out.println(response.getStatusLine());
        }

        String responseStr = inputStreamToString(response.getEntity().getContent());
        JSONObject responseJson = null;
        try {
            responseJson = new JSONObject(responseStr);
        } catch (JSONException e) {
            System.out.println(responseStr);
            e.printStackTrace();
        }
        return responseJson;
    }

    private static void printCouches(JSONObject responseJson) throws Exception {
        JSONArray coaches = responseJson.optJSONArray("coaches");
        if (coaches != null) {
            for (Object couch : coaches.toList()) {
                System.out.println("\t" + couch);
            }
        } else {
            System.out.println("\t" + responseJson);
        }
    }

    private static void sendEmail() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("login", "pass");
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("from@no-spam.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("tmp@mail.com"));
            message.setSubject("Testing Subject");
            message.setText("Dear Mail Crawler," + "\n\n No spam to my email, please!");

            Transport.send(message);

            System.out.println("Done");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
