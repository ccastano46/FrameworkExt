package eci.arem.server;

import eci.arem.http.HttpRequest;
import eci.arem.http.HttpResponse;
import eci.arem.http.HttpStatus;
import eci.arem.service.StaticFileService;
import eci.arem.server.utils.JsonUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Router {

    private static final Map<String, WebService> services = new HashMap<>();

    private Router(){}

    public static void register(String route, WebService service){
        services.put(route, service);
    }

    public static HttpResponse invokeService(HttpRequest request, HttpResponse response){
        WebService service = services.get(request.getPath());
        service.invoke(request, response);
        return response;
    }

    public static HttpResponse route(HttpRequest request){
        HttpResponse response = new HttpResponse();
        try{
            if (!request.getMethod().equals("GET")) return invokeBadMethod(request, response);
            if (request.getPath().equals("/badRequest")) return invokeBadRequest(response);
            if (isAService(request)) return invokeService(request, response);
            return  StaticFileService.invokeStaticFiles(request, response);
        }catch(FileNotFoundException _){
            return invokeNotFound(request, response);
        }catch(IOException e){
            return invokeError(e.getMessage(), response);
        }
    }

    public static HttpResponse invokeNotFound(HttpRequest req, HttpResponse response){
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.setBody(("404 Not Found: " + JsonUtils.escapeJson(req.getPath()))
                .getBytes(StandardCharsets.UTF_8));
        return response;
    }

    public static HttpResponse invokeError(String error, HttpResponse response){
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setBody(("500 Internal Server Error: " + JsonUtils.escapeJson(error))
                .getBytes(StandardCharsets.UTF_8));
        return response;
    }

    public static HttpResponse invokeBadMethod(HttpRequest request, HttpResponse response){
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.setBody(("400 Bad Request: method not allowed: " + request.getMethod()).getBytes(StandardCharsets.UTF_8));
        return response;
    }

    public static HttpResponse invokeBadRequest(HttpResponse response){
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.setBody(("400 Bad Request").getBytes(StandardCharsets.UTF_8));
        return response;
    }

    public static boolean isAService(HttpRequest route){
        return services.containsKey(route.getPath());
    }
}
