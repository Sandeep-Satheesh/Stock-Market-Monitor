package utils;

import com.google.gson.JsonObject;
import utils.listeners.OnHTTPRequestCompletedListener;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HTTPHelper {
    //NOTE: ALL METHODS OF THIS CLASS MUST BE ACCESSED FROM BACKGROUND THREAD TO AVOID NetworkOnMainThreadException.

    public static void sendRequest(String reqURL, String requestMethod, HashMap<String, String> headers, int readTimeout, int connectTimeout, boolean doInput, boolean doOutput, JsonObject params, OnHTTPRequestCompletedListener onHTTPRequestCompletedListener) {
        new Thread(() -> {
            URL u;
            String url = reqURL;
            HttpsURLConnection conn;
            try {
                //GET reqs can't have body, therefore dataToWrite => params of GET
                if (requestMethod.equalsIgnoreCase("get") && params != null) {
                    url += "?";
                    StringBuilder urlBuilder = new StringBuilder(url);
                    for (String key : params.keySet())
                        urlBuilder.append(key).append("=").append(params.get(key).getAsString()).append("&");
                    urlBuilder.setLength(urlBuilder.length() - 1);
                    url = urlBuilder.toString();
                }
                u = new URL(url);

                conn = (HttpsURLConnection) u.openConnection();
                conn.setRequestMethod(requestMethod.toUpperCase());

                if (headers != null)
                    for (String k : headers.keySet())
                        conn.setRequestProperty(k, headers.get(k));

                if (readTimeout > 0)
                    conn.setReadTimeout(readTimeout);
                if (connectTimeout > 0)
                    conn.setConnectTimeout(connectTimeout);

                conn.setDoInput(doInput);
                conn.setDoOutput(doOutput);

                try {
                    conn.connect();
                } catch (Exception e) {
                    onHTTPRequestCompletedListener.onRequestFailed(-1, e.getMessage() + "\nReported Cause: " + e.getCause());
                    return;
                }
                if (requestMethod.equalsIgnoreCase("post")) {
                    if (params != null) {
                        PrintWriter writer = new PrintWriter(conn.getOutputStream());
                        writer.write(params.toString());
                        writer.flush();
                        writer.close();
                    }
                }

                int status = conn.getResponseCode();
                if (status != 200 || conn.getErrorStream() != null) {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String responseLine;

                        while ((responseLine = br.readLine()) != null)
                            response.append(responseLine.trim());

                        conn.disconnect();
                        onHTTPRequestCompletedListener.onRequestFailed(status, response.toString());
                    } catch (Exception e) {
                        conn.disconnect();
                        onHTTPRequestCompletedListener.onRequestFailed(-1, "ErrCode: " + status + ", could not open error stream");
                    }
                    return;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;

                while ((responseLine = br.readLine()) != null)
                    response.append(responseLine.trim());

                conn.disconnect();
                onHTTPRequestCompletedListener.onRequestSuccess(status, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                onHTTPRequestCompletedListener.onRequestFailed(-1, e.getMessage() + "\nReported Cause: " + e.getCause());
            }
        }).start();
    }

    public static String sendSynchronousRequest(String reqURL, String requestMethod, HashMap<String, String> headers, int readTimeout, int connectTimeout, boolean doInput, boolean doOutput, JsonObject params) {
        URL u;
        String url = reqURL;
        HttpsURLConnection conn;
        try {
            //GET reqs can't have body, therefore dataToWrite => params of GET
            if (requestMethod.equalsIgnoreCase("get") && params != null) {
                url += "?";
                StringBuilder urlBuilder = new StringBuilder(url);
                for (String key : params.keySet())
                    urlBuilder.append(key).append("=").append(params.get(key).getAsString()).append("&");
                urlBuilder.setLength(urlBuilder.length() - 1);
                url = urlBuilder.toString();
            }
            u = new URL(url);

            conn = (HttpsURLConnection) u.openConnection();
            conn.setRequestMethod(requestMethod.toUpperCase());

            if (headers != null)
                for (String k : headers.keySet())
                    conn.setRequestProperty(k, headers.get(k));

            if (readTimeout > 0)
                conn.setReadTimeout(readTimeout);
            if (connectTimeout > 0)
                conn.setConnectTimeout(connectTimeout);

            conn.setDoInput(doInput);
            conn.setDoOutput(doOutput);

            try {
                conn.connect();
            } catch (Exception e) {
                return e.toString();
            }
            if (requestMethod.equalsIgnoreCase("post")) {
                if (params != null) {
                    PrintWriter writer = new PrintWriter(conn.getOutputStream());
                    writer.write(params.toString());
                    writer.flush();
                    writer.close();
                }
            }

            int status = conn.getResponseCode();
            if (status != 200 || conn.getErrorStream() != null) {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;

                    while ((responseLine = br.readLine()) != null)
                        response.append(responseLine.trim());

                    conn.disconnect();

                } catch (Exception e) {
                    conn.disconnect();
                    return null;
                }
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;

            while ((responseLine = br.readLine()) != null)
                response.append(responseLine.trim());

            conn.disconnect();

            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}