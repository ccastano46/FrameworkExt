package eci.arem.http;


import java.net.URI;
import java.nio.charset.StandardCharsets;


public class HttpRequest {
    private final String method;
    private final URI uri;

    public HttpRequest(String method, URI requestURI) {
        this.method = method;
        this.uri = requestURI;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return uri.getPath();
    }

    public String getQuery() {
        return uri.getQuery();
    }

    public String getValue(String key) {
        String query = getQuery();
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}