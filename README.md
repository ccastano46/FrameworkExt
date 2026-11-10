# FrameworkExt

## Project title and description

**FrameworkExt** is an extension of the [WebFramework](https://github.com/ccastano46/WebFramework.git) project for the **Enterprise Architectures** course at **Escuela Colombiana de Ingeniería Julio Garavito**. It preserves the original lightweight web framework and its class architecture, while adding two capabilities: containerization with Docker and concurrent request handling in `HttpServer` through Java virtual threads. The framework keeps request parsing, response building, static file serving, and error responses internal, letting the developer define application behavior in `eci.arem.app.Application` class.

As in the last project, the application includes a browser client for the services `/hello`, `/square`, `/time`, and `/health`. The browser client sends asynchronous `GET` requests and updates the page without performing a full reload. The same server can also deliver static resources, such as HTML, CSS files, JavaScript files, and PNG images, through the static-file route.

The application can be packaged as an executable JAR and run inside the Docker image defined in the repository's `Dockerfile`. The entry point remains `eci.arem.app.Application`.

The application reads the following environment variables at startup. If a variable is not defined, the corresponding default value is used:

| Variable | Purpose | Default Value |
| --- | --- | --- |
| `PORT` | TCP port the HTTP server listens on | `8080` |
| `STATIC_PATH` | Root directory where static resources are located | `src/main/resources/public` |
| `APP_ENV` | Execution environment | `development` |

> When `APP_ENV` is set to `production`, the `/shutdown` endpoint is disabled to prevent remote termination of the server.

## System metaphor and architecture

### Architecture Metaphor

The framework can be understood as a **single-entrance office building organized as a modern service center**. A visitor (the browser) arrives at the front door and hands over a request. The building has one entrance (`ServerSocket`) and one directory (`Router`), but it does not force every visitor to wait behind a single receptionist. As soon as a visitor arrives, the entrance coordinator accepts the connection and assigns it a lightweight virtual attendant.

Each attendant carries one visitor's form (`HttpRequest`) from the entrance to the appropriate office and brings the response envelope (`HttpResponse`) back. The attendant is not an employee who permanently occupies a desk: if the office has to wait for information from the network or for a slow operation to finish, the attendant can move to a waiting area. The platform worker that was carrying it is released to attend another visitor, and the same attendant resumes the original conversation when the response is ready. This is the role of the virtual threads implemented by `HttpServer`.

The result is a building that can keep many conversations in progress at the same time without creating one heavy, permanently occupied platform worker per visitor. Fast requests can continue through the offices while slower visitors are waiting, and the response for each visitor still returns through its own connection. The building remains intentionally simple—one entrance, one directory, and the same offices as the original framework—but its waiting room can absorb much more concurrent work.

| Building metaphor | Framework component |
| --- | --- |
| **Building entrance** | `HttpServer`: the front door that accepts visitors through a `ServerSocket`. It reads who they are and what they need (`parse`), then assigns each connection to a lightweight virtual attendant before passing it to the lobby directory. |
| **Lobby directory** | `Router`: the board in the lobby that lists which offices are available. It checks the visitor's request against the registered offices (`Map<String, WebService>`). If the office exists, the visitor is escorted there. If not, it checks the document archive. If nothing matches, the visitor receives a "not found" notice. If the visitor entered through the wrong door (wrong HTTP method), they are turned away at the lobby. |
| **Individual offices** | Lambda handlers registered via `get("/path", (req, resp) -> {...})`: each office handles a specific service. The `/hello` office writes greetings, the `/square` office computes numbers, the `/time` office reads the building clock. Each office fills out a response form (`HttpResponse`) and hands it back through the lobby. |
| **Document archive** | `StaticFileService` and `FileResolver`: when no office handles the request, the directory sends the visitor to the archive room, which stores HTML, CSS, JavaScript, and images. The archivist locates the file, stamps it with its type, and delivers it directly — no office visit required. |
| **The visitor's form** | `HttpRequest`: the form each visitor fills out at the entrance, their name (method), where they want to go (path), and any special notes (query parameters). This form is passed unchanged to whoever serves them. |
| **The response envelope** | `HttpResponse`: every answer, whether from an office or the archive, is placed in the same kind of envelope. It carries the content (body), a label (content type), and a status stamp (status code). The header is printed on the envelope just before it is handed back to the visitor. |
| **The building directory sign** | `WebFramework`: the public-facing sign outside that advertises the building's services. The building manager (`Application.java`) reads this sign to know what can be configured: `get()` to open a new office, `staticfiles()` to stock the archive, `start()` to open the building, and `stop()` to close it. |
| **Building regulations** | Environment variables (`PORT`, `STATIC_PATH`, `APP_ENV`): external rules that govern how the building operates. Changing the port is like changing the building's street address. Setting `APP_ENV=production` locks the emergency exit. The `/shutdown` door is disabled, and only the building administrator can close the building from outside. |
| **Closing procedure** | Graceful shutdown: when the `/shutdown` office is visited (only in development), the server closes the listening door, stops accepting new visitors, and waits for in-flight virtual attendants to finish before the building closes. |

### Architecture diagram

The following UML diagram represents the main classes and relationships in the framework architecture.

![architecture.png]

## Component responsibilities

| Component | Responsibility | Current implementation |
| --- | --- | --- |
| Browser client | Presents the laboratory interface, validates basic input, builds service URLs, sends asynchronous requests, displays loading states, and separates successful results from errors. | `src/main/resources/public/async-client.html`, `scripts/async-client.js`, and `styles/async-client.css` |
| `Application` | Acts as the composition root: reads environment variables (`PORT`, `STATIC_PATH`, `APP_ENV`), registers all lambda-based routes via `WebFramework.get()`, conditionally enables `/shutdown` in development mode, and starts the server. | `eci.arem.app.Application` |
| `WebFramework` | Exposes `get()`, `staticFiles()`, `start()`, and `stop()`. Delegates route registration to `Router`, static file configuration to `StaticFileService`, and server lifecycle to `HttpServer`. Contains no logic of its own. | `eci.arem.webframework.WebFramework` |
| `HttpServer` | Opens the listening socket on the configured port, accepts client connections in a loop, and submits each connection to `Executors.newVirtualThreadPerTaskExecutor()`. Each virtual thread parses the raw request line, delegates routing to `Router`, writes the response, and closes the connection. It isolates each connection with `try-with-resources` so a single failure does not terminate the server. | `eci.arem.server.HttpServer` |
| `Router` | Owns the route registry (`Map<String, WebService>`), evaluates each incoming request against registered routes, and delegates to the matching lambda handler. If no route matches, falls back to `StaticFileService`. Handles error responses for bad methods, bad requests, not-found resources, and internal errors. | `eci.arem.server.Router` |
| `WebService` | Functional interface that defines the contract for lambda handlers: `void invoke(HttpRequest request, HttpResponse response)`. Each registered route is an implementation of this interface. | `eci.arem.server.WebService` |
| `HttpRequest` | Represents a parsed HTTP request. Holds the method, path, and query string extracted from the request line. Provides `getValue(key)` to retrieve and URL-decode individual query parameters. | `eci.arem.http.HttpRequest` |
| `HttpResponse` | Represents an HTTP response. Carries the status code, content type, and body as `byte[]`. Builds the raw HTTP header (`status line + Content-Type + Content-Length` ) on demand via `getHeader()`. Defaults to `200 OK` and `text/plain`. | `eci.arem.http.HttpResponse` |
| `HttpStatus` | Enum of standard HTTP status codes (`OK`, `BAD_REQUEST`, `NOT_FOUND`, `INTERNAL_SERVER_ERROR`, etc. ) with numeric code and description. Used by `HttpResponse` to build the status line. | `eci.arem.http.HttpStatus` |
| `StaticFileService` | Serves static resources when no dynamic route matches. Reads file bytes through `FileResolver`, determines the content type, and maps `/` to the default page. Delegates the base path configuration from `WebFramework.staticFiles( )`. | `eci.arem.service.StaticFileService` |
| `FileResolver` | Resolves a requested path against the configured base directory, rejects paths that escape it (path traversal protection), reads the file bytes from disk, and determines the MIME type based on file extension. | `eci.arem.service.FileResolver` |
| Public resources | Static files served by the Java server: HTML pages, stylesheets, JavaScript modules, and images that compose the browser-side interface. | `src/main/resources/public/` |

### Supported request surface

The server exposes a deliberately limited request surface. The route definitions are represented below with general parameter placeholders.

| Method | URL |
| --- | --- |
| `GET` | `/` |
| `GET` | `/hello?name=[name]` |
| `GET` | `/square?value=[number]` |
| `GET` | `/time` |
| `GET` | `/health` |
| `GET` | `/shutdown` |
| `GET` | `/index.html` and other supported static-resource paths |

A missing required `name` or `value` parameter returns `400 Bad Request`. A non-`GET` method returns `400 Bad Request`. A requested static file that does not exist returns `404 Not Found`. An unrecognized service URL reaches the fallback bad-request route and returns `404 Not Found`.

## Concurrency model

This extension keeps the original class architecture but changes how `HttpServer` processes accepted connections. The accept loop remains responsible for receiving sockets; instead of processing each request directly in that loop, it submits each socket to `Executors.newVirtualThreadPerTaskExecutor()`. Each connection therefore runs in its own virtual thread.

### What are virtual threads?

A virtual thread is a lightweight thread managed by the Java Virtual Machine (JVM). Unlike a traditional platform thread, it is not permanently tied to one operating system thread and does not reserve a full platform-thread stack for its entire lifetime.

When a request is blocked waiting for network input/output, a response from another service, or a delay such as the `/slow` endpoint, the JVM parks the virtual thread and releases its carrier platform thread. The carrier can then execute another request. In practical terms, a request waiting for a response does not keep an entire OS thread and its stack occupied, allowing the server to handle many simultaneous connections with lower resource consumption.

| Platform threads | Virtual threads |
| --- | --- |
| Heavier OS-managed resource. | Lightweight JVM-managed task. |
| A blocked request keeps its platform thread occupied. | A blocked request is parked and its carrier can execute another virtual thread. |
| The number of simultaneous requests is more tightly limited by available threads. | Many concurrent I/O-bound requests can be handled with fewer platform threads. |

Virtual threads do not remove every resource limit: each connection still uses a socket and some memory, and CPU-bound work still consumes CPU. Their benefit is reducing the cost of waiting, which is especially useful for HTTP workloads that spend time waiting for I/O. During shutdown, the executor is closed so in-flight requests can finish before the server stops.

## Prerequisites

- **Java 25** (JDK) to compile and run the server.

- **Maven 3.9+** to compile and package the project.

- **Docker Desktop** or **Docker Engine** to build and run the container.

- **Postman or other service** to test the server.

- **A browser** to test the client.

## Dockerfile

The repository includes the following `Dockerfile`:

```dockerfile
FROM amazoncorretto:25

WORKDIR /app

COPY target/FrameWorkExtension-1.0-SNAPSHOT.jar app.jar
COPY src/main/resources/public public

ENV STATIC_PATH=public

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

The Dockerfile describes how to build the image that packages and runs the application:

| Instruction | Purpose |
| --- | --- |
| `FROM amazoncorretto:25` | Selects Amazon Corretto 25 as the base image. It provides the Java runtime required by the application. |
| `WORKDIR /app` | Creates `/app` inside the image and makes it the working directory for the following instructions and for the process that starts the application. |
| `COPY target/FrameWorkExtension-1.0-SNAPSHOT.jar app.jar` | Copies the JAR produced by `mvn clean package` from the local `target/` directory into the image and renames it to `app.jar`. |
| `COPY src/main/resources/public public` | Copies the static browser resources into `/app/public`, where the application can serve them. |
| `ENV STATIC_PATH=public` | Defines the default path used by the application to locate static resources inside the container. |
| `EXPOSE 8080` | Documents that the application listens on container port `8080`. It does not publish the port to the host; publishing is done with `-p 35000:8080` in `docker run`. |
| `ENTRYPOINT ["java", "-jar", "app.jar"]` | Defines the command executed when the container starts. It launches the packaged application with Java. |

## How to run the project locally with Docker

The image is built for two CPU architectures so it runs natively both on a 
linux or MacOS (linux/arm64) and on an x86 EC2 instance such as 
t3.micro (linux/amd64). 
Both variants are published under the same tag, and each machine automatically 
pulls the one that matches its processor.
1. Package the executable JAR with Maven:

```bash
mvn clean package
```

2. Log in to Docker Hub.
```bash
docker login
```
3. Build the image and push it to Docker Hub:

```bash
docker buildx build --platform linux/amd64,linux/arm64 -t ccastano46/framework-extension:dev --push .
```

> To create the image for prod just add ENV APP_ENV=prod in the Dockerfile and repeat
> step 1 and 3 but with the tag framework-extension:prod

4. Run the image in a container and verify that it is running:

> For this study case we are going to run two containers, one for dev and one for prod.

```bash
docker run -d -p 35000:8080 --name framework ccastano46/framework-extension:dev
docker run -d -p 34000:8080 --name framework ccastano46/framework-extension:dev
docker ps
```

5. Access the application locally:

```
http://localhost:35000/
http://localhost:34000/
```

You can also verify the health endpoint from the terminal:

```bash
curl http://localhost:35000/health
```

6. Stop and remove the container:

```bash
docker rm -f frameworkext-docker
```

## How to use the application

The path `/` returns the static HTML file `async-client.html`, which provides the browser client. The client sends asynchronous `GET` requests to the services documented in the supported request surface and updates the page without performing a full reload.

### Local demonstration video

https://drive.google.com/file/d/1jzn_WKrlBnXeHMIIiCiline0wKmi9UH9/view?usp=sharing

## Cloud deployment

https://drive.google.com/file/d/1bnjmksqpXxV0nw3uCCmCEgcAMKTbSIUU/view?usp=sharing
## Project structure

The repository follows the standard Maven layout. The Java server code is under `src/main/java`, the resources served to browsers are under `src/main/resources/public`, and the root `Dockerfile` packages the executable JAR together with the public resources. The architecture of the Java classes remains the same as in `WebFramework`; the extension adds Docker-based packaging and virtual-thread execution inside `HttpServer`.

```
FrameworkExt/
├── .gitignore
├── README.md
├── Dockerfile
├── pom.xml
├── docs/
│   ├── HTTPServer.postman_collection.json
│   └── images/
└── src/
    └── main/
        ├── java/
        │   └── eci/arem/
        │       ├── app/
        │       │   └── Application.java
        │       ├── http/
        │       │   ├── HttpRequest.java
        │       │   ├── HttpResponse.java
        │       │   └── HttpStatus.java
        │       ├── server/
        │       │   ├── HttpServer.java
        │       │   ├── Router.java
        │       │   ├── WebService.java
        │       │   └── utils/
        │       │       └── JsonUtils.java
        │       ├── service/
        │       │   ├── FileResolver.java
        │       │   └── StaticFileService.java
        │       └── webframework/
        │           └── WebFramework.java
        └── resources/
            └── public/
                ├── async-client.html
                ├── index.html
                ├── images/
                ├── scripts/
                └── styles/
```

## Testing Concurrency

Just for the tests, we created a new lambda function that sleeps for 3 seconds, it is
invoked by the `/slow` endpoint. After the tests, this lambda function can be removed.

#### 1. Two slow requests in parallel

**What is being tested:** Two requests that each wait three seconds can be processed concurrently by virtual threads.

```bash
time (curl -sS localhost:35000/slow & curl -sS localhost:35000/slow & wait)
```

**Expect:** approximately `3 s` total instead of approximately `6 s` sequentially.

#### 1b. Ten slow requests in parallel

**What is being tested:** The server can keep several slow requests in progress without processing them one after another.

```bash
time (for i in {1..10}; do curl -sS localhost:35000/slow & done; wait)
```

**Expect:** approximately `3 s` total instead of approximately `30 s` sequentially.

#### 2. A slow request does not block a fast request

**What is being tested:** A fast `/square` request can finish while another virtual thread is waiting on `/slow`.

```bash
curl -sS localhost:35000/slow & time curl -sS "localhost:35000/square?value=5"
```

**Expect:** `/square` completed while `/slow` was still running.

#### 3. Concurrent load

**What is being tested:** The server handles a high number of concurrent health requests without failures.

```bash
ab -n 2000 -c 100 http://localhost:35000/health
```

**Expected:** Passed: `0` failures and approximately `1,627 req/s`.

#### 4. Graceful shutdown with a request in progress

**What is being tested:** The server stops accepting new connections but allows a request already in progress to finish.

```bash
curl -sS localhost:35000/slow & sleep 0.5; curl -sS localhost:35000/shutdown; wait
```

**Expected:** `Goodbye`, followed by completion of `/slow`.

#### 5. Confirm that the server no longer accepts connections

**What is being tested:** After graceful shutdown, the listening socket is closed.

```bash
curl localhost:35000/health
```

