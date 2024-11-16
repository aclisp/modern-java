package com.mycompany.app;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.mycompany.util.JUL;

public class WebClient {

  static {
    JUL.initLogging();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:8080"))
        .build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

    System.out.println(response.body());
  }
}
