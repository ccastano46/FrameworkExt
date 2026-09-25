package eci.arem.server;

import eci.arem.http.HttpRequest;
import eci.arem.http.HttpResponse;

public interface WebService {
    public void invoke(HttpRequest request, HttpResponse response);
}
