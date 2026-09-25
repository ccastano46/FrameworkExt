package eci.arem.server;


import eci.arem.http.HttpRequest;
import eci.arem.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.io.*;


public class HttpServer {
    private static boolean running = false;
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    public static void main(String[] args) throws IOException, URISyntaxException {

        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);
        running = true;

        while (running) {
            logger.info("Ready to receive...");
            try {
                Socket clientSocket = serverSocket.accept();
                try (clientSocket;
                     OutputStream out = clientSocket.getOutputStream();
                     BufferedReader in = new BufferedReader(
                             new InputStreamReader(clientSocket.getInputStream()))) {

                    HttpRequest request = parse(in);
                    HttpResponse response = Router.route(request);

                    out.write(response.getHeader());
                    out.write(response.getBody());
                    out.flush();
                }
            } catch (Exception e) {
                logger.error("Error handling connection: {}", e.getMessage());
            }
        }
        logger.info("Server stopped gracefully.");
        serverSocket.close();
    }

    private static HttpRequest parse(BufferedReader in) throws IOException, URISyntaxException {
        boolean isFirstLine = true;
        String method = "";
        String inputLine;
        String strUri = "";

        while ((inputLine = in.readLine()) != null) {
            if (isFirstLine) {
                String[] parts = inputLine.split(" ");
                method = parts.length > 0 ? parts[0] : "";

                strUri = parts.length > 1 ? parts[1] : "/badRequest";

                isFirstLine = false;
            }
            logger.info("Received: {}", inputLine);
            if (!in.ready()) {
                break;
            }
        }

        return new HttpRequest(method, new URI(strUri));
    }

    public static void stop() {
        running = false;
    }

}