package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Main {

    private static final String BASE_URL = "https://www.okx.com";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final SecretsLoader secretsLoader = new SecretsLoader();

    public static void main(String[] args) {
        System.out.println("Hello world!");

        try {
            var price = getBTCAveragePrice();
            buyBTC(price);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred: " + e.getMessage());
        }
    }

    //買い注文と売り注文の平均価格を計算
    private static float getBTCAveragePrice() throws IOException, InterruptedException {
        String url = BASE_URL + "/api/v5/market/books?instId=BTC-USDT";
        String response = sendRequest(url);

        JSONObject obj = new JSONObject(response);
        JSONArray data = obj.getJSONArray("data");
        JSONObject firstEntry = data.getJSONObject(0);

        // Asks
        JSONArray asks = firstEntry.getJSONArray("asks");
        float askPrice = Float.parseFloat(asks.getJSONArray(0).getString(0));

        // Bids
        JSONArray bids = firstEntry.getJSONArray("bids");
        float bidPrice = Float.parseFloat(bids.getJSONArray(0).getString(0));

        // Calculate the average
        float averagePrice = (askPrice + bidPrice) / 2;
        System.out.println("Average Price: " + averagePrice);
        return averagePrice;
    }

    // 0.00001個のBTCを購入
    private static void buyBTC(float price) throws IOException, InterruptedException {
        String method = "POST";
        String requestPath = "/api/v5/trade/order";
        String body = String.format("{\"instId\":\"BTC-USDT\",\"tdMode\":\"cash\",\"side\":\"buy\",\"ordType\":\"limit\",\"px\":\"%f\",\"sz\":\"0.00001\"}", price);

        String response = sendAuthenticatedRequest(requestPath, method, body);
        System.out.println(response);
    }

    private static String sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-simulated-trading", "1");

        HttpRequest request = builder.GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String sendAuthenticatedRequest(String endPoint, String method, String body) throws IOException, InterruptedException {
        String timestamp = ZonedDateTime.now(java.time.Clock.systemUTC()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        String concatenatedString = timestamp + method + endPoint + body;

        String signature = calculateHMAC(concatenatedString, secretsLoader.getSecretKey());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL+endPoint))
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-simulated-trading", "1")
                .header("OK-ACCESS-KEY", secretsLoader.getAccessKey())
                .header("OK-ACCESS-SIGN", signature)
                .header("OK-ACCESS-TIMESTAMP", timestamp)
                .header("OK-ACCESS-PASSPHRASE", secretsLoader.getAccessPassphrase())
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    //HmacSHA256を計算してBase64で返す
    private static String calculateHMAC(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }
}

