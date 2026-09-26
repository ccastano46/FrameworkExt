package eci.arem.server;

import eci.arem.http.HttpRequest;
import eci.arem.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {

    private static volatile boolean running = false;
    private static volatile ServerSocket serverSocket;
    private static final int READ_TIMEOUT_MS = 5000;

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private HttpServer() {}

    public static void main(String[] args) throws IOException, URISyntaxException {
        int port = Integer.parseInt(args[0]);


        try (ServerSocket socket = new ServerSocket(port);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            serverSocket = socket;
            running = true;
            logger.info("Server listening on port {}", port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.execute(() -> handleRequest(clientSocket));
                } catch (SocketException e) {
                    if (!running) break;
                    logger.error("Error accepting connection: {}", e.getMessage());
                } catch (IOException e) {
                    logger.error("Error accepting connection: {}", e.getMessage());
                }
            }
            logger.info("Waiting for in-flight requests to finish...");
        }
        logger.info("Server stopped gracefully.");
    }

    private static void handleRequest(Socket clientSocket) {
        try (clientSocket;
             OutputStream out = clientSocket.getOutputStream();
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream()))) {

            clientSocket.setSoTimeout(READ_TIMEOUT_MS);
            HttpRequest request = parse(in);
            HttpResponse response = Router.route(request);

            out.write(response.getHeader());
            out.write(response.getBody());
            out.flush();
        } catch (Exception e) {
            logger.error("Error handling connection: {}", e.getMessage());
        }
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
        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket: {}", e.getMessage());
            }
        }
    }
}