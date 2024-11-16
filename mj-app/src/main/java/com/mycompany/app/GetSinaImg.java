package com.mycompany.app;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;

public class GetSinaImg {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        var request = HttpRequest.newBuilder()
                .uri(new URI("https://wx4.sinaimg.cn/orj360/007s41inly1hr34fwk3c1j30pr0lznfh.jpg"))
                .GET()
                .build();

        var client = HttpClient.newHttpClient();
        var response = client.send(request, BodyHandlers.ofFile(Paths.get("img")));

        System.out.println(response.toString());
        //System.out.println(response.body());
    }
}
