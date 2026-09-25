package eci.arem.http;

public class HttpResponse {
    private HttpStatus status;
    private String contentType;
    private byte[] body;
    private byte[] header;

    public HttpResponse(){
        this.status = HttpStatus.OK;
        this.contentType = "text/plain";
    }

    public HttpStatus getStatusCode() {
        return status;
    }

    public void setStatusCode(HttpStatus status) {
        this.status = status;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getHeader() {
        buildHeader();
        return header;
    }

    private void buildHeader() {
        header = ("HTTP/1.1 " + status.getCode() + " " + status.getDescription() + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + body.length + "\r\n\r\n").getBytes();
    }
}