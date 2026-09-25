package eci.arem.app;

import static eci.arem.webframework.WebFramework.stop;
import static eci.arem.webframework.WebFramework.staticFiles;
import static eci.arem.webframework.WebFramework.start;
import static eci.arem.webframework.WebFramework.get;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import eci.arem.http.HttpStatus;

public class Application {

    static void main(String[] args) throws Exception {
        Map<String, String> env = System.getenv();

        //String staticPath = env.getOrDefault("STATIC_PATH", "public");
        String staticPath = env.getOrDefault("STATIC_PATH", "src/main/resources/public");
        String environment = env.getOrDefault("APP_ENV", "development");
        int port = Integer.parseInt(env.getOrDefault("PORT", "8080"));
        staticFiles(staticPath);

        get("/hello",
                (req, resp) ->{
                    String name = req.getValue("name");
                    if (name == null || name.isBlank()) {
                        resp.setStatusCode(HttpStatus.BAD_REQUEST);
                        resp.setBody(("Bad request: missing 'name' parameter").getBytes(StandardCharsets.UTF_8));
                    } else {
                        resp.setBody(("Hello " + name).getBytes(StandardCharsets.UTF_8));
                    }
                }

        );

        get("/square",
                (req, resp) ->{
                    String rawValue = req.getValue("value");
                    String strBody = "";
                    try {
                        double value = Double.parseDouble(rawValue);
                        double square = value * value;
                        resp.setContentType("application/json");
                        strBody = "{\"value\":" + value + ",\"square\":" + square + "}";
                    } catch (NumberFormatException | NullPointerException _) {
                        resp.setStatusCode(HttpStatus.BAD_REQUEST);
                        strBody = "Bad request: missing or invalid 'value' parameter";
                    }
                    resp.setBody(strBody.getBytes(StandardCharsets.UTF_8));
                }

        );
        get("/health", (req, resp) ->{
            resp.setContentType("application/json");
            resp.setBody("{\"status\":\"OK\"}".getBytes(StandardCharsets.UTF_8));
        });

        get("/time", (req, resp) ->{
           resp.setContentType("application/json");
           resp.setBody(("{\"serverTime\":\"" + Instant.now() + "\"}").getBytes(StandardCharsets.UTF_8));
        });

        get("/pi", (req, resp) ->
                resp.setBody((String.valueOf(Math.PI)).getBytes(StandardCharsets.UTF_8))
                );

        get("/e", (req, resp) ->
                resp.setBody((String.valueOf(Math.E)).getBytes(StandardCharsets.UTF_8))
        );

        if(environment.equals("development") || environment.equals("dev")){
            get("/shutdown", (req, resp) -> {
                resp.setBody(("Goodbye").getBytes(StandardCharsets.UTF_8));
                stop();
            });
        }

        start(port);
    }
}

