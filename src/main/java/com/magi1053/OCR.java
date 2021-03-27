package com.magi1053;

import com.google.gson.Gson;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

class OCR {

    private String apiKey;
    private final boolean scale = false;
    private final boolean isTable = true;
    private final int OCREngine = 2;
    private CloseableHttpClient httpClient = HttpClients.createDefault();

    OCR(String apiKey) {
        this.apiKey = apiKey;
    }

    Response sendGet(String imageUrl) throws Exception {
        URIBuilder url = new URIBuilder("https://api.ocr.space/parse/imageurl");
        url.addParameter("apikey", apiKey);
        url.addParameter("scale", String.valueOf(scale));
        url.addParameter("isTable", String.valueOf(isTable));
        url.addParameter("OCREngine", String.valueOf(OCREngine));
        url.addParameter("url", imageUrl);
        HttpGet get = new HttpGet(url.build());
        CloseableHttpResponse response = httpClient.execute(get);
        String responseString = EntityUtils.toString(response.getEntity());
        return new Gson().fromJson(responseString, Response.class);
    }

    Response sendPost(String imageUrl) throws Exception {
        HttpPost post = new HttpPost("https://api.ocr.space/parse/image");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("apikey", apiKey));
        params.add(new BasicNameValuePair("scale", String.valueOf(scale)));
        params.add(new BasicNameValuePair("isTable", String.valueOf(isTable)));
        params.add(new BasicNameValuePair("OCREngine", String.valueOf(OCREngine)));
        params.add(new BasicNameValuePair("url", imageUrl));
        post.setEntity(new UrlEncodedFormEntity(params));
        CloseableHttpResponse response = httpClient.execute(post);
        String responseString = EntityUtils.toString(response.getEntity());
        return new Gson().fromJson(responseString, Response.class);
    }

    static class Response {
        Object[] ParsedResults;
    }
}


