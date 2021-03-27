package com.magi1053;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Date;

class Sheets {

    private String apiKey;
    private String url;
    private CloseableHttpClient httpClient;

    Sheets(String scriptId, String apiKey) {
        this.url = String.format("https://script.google.com/macros/s/%s/exec", scriptId);
        this.apiKey = apiKey;
        this.httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
    }

    @SneakyThrows
    Response sendGet(String user) {
        System.out.println("Fetching data from spreadsheet");
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.addParameter("token", apiKey);
        uriBuilder.addParameter("search", user);
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        return sendRequest(httpGet);
    }

    Response sendPost(String user) {
        return sendPost(user, null);
    }

    @SneakyThrows
    Response sendPost(String user, HttpEntity params) {
        System.out.println("Sending data to spreadsheet");
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.addParameter("token", apiKey);
        uriBuilder.addParameter("search", user);
        HttpPost httpPost = new HttpPost(uriBuilder.build());
        httpPost.setEntity(params);
        return sendRequest(httpPost);
    }

    private Response sendRequest(HttpRequestBase httpRequest) throws IOException {
        CloseableHttpResponse httpResponse = httpClient.execute(httpRequest);
        String responseString = EntityUtils.toString(httpResponse.getEntity());
        GsonBuilder gsonBuilder = new GsonBuilder();
        // Register an adapter to manage the date types as long values
        gsonBuilder.registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsJsonPrimitive().getAsLong()));
        Response sheetsResponse = gsonBuilder.create().fromJson(responseString, Response.class);
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(sheetsResponse));
        return sheetsResponse;
    }

    static class Response {
        Character user;
        String error;
    }
}

