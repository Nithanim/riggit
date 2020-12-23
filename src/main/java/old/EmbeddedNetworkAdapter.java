package old;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import lombok.SneakyThrows;
import net.dean.jraw.http.BasicAuthData;
import net.dean.jraw.http.HttpRequest;
import net.dean.jraw.http.HttpResponse;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.UserAgent;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;

public class EmbeddedNetworkAdapter implements NetworkAdapter {
  private UserAgent userAgent;

  public EmbeddedNetworkAdapter(UserAgent userAgent) {
    this.userAgent = userAgent;
  }

  @NotNull
  @Override
  public UserAgent getUserAgent() {
    return this.userAgent;
  }

  @Override
  public void setUserAgent(@NotNull UserAgent userAgent) {
    this.userAgent = userAgent;
  }

  @NotNull
  @Override
  public WebSocket connect(@NotNull String s, @NotNull WebSocketListener webSocketListener) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  @SneakyThrows
  public HttpResponse execute(@NotNull HttpRequest oldHttpRequest) {
    HttpClient.Builder hb;
    var httpClient = hb = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
    if (oldHttpRequest.getBasicAuth() != null) {
      BasicAuthData ba = oldHttpRequest.getBasicAuth();
      hb.authenticator(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(ba.getUsername(), ba.getPassword().toCharArray());
            }
          });
    }
    hb.version(HttpClient.Version.HTTP_1_1).build();
    HttpClient client = httpClient.build();

    java.net.http.HttpRequest newHttpRequest = convertRequest(oldHttpRequest);

    var newHttpResponse =
        client.send(newHttpRequest, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
    Response oldHttpResponse = convertResponse(newHttpResponse);

    return new HttpResponse(oldHttpResponse);
  }

  private Response convertResponse(java.net.http.HttpResponse<byte[]> newHttpResponse) {
    Response.Builder b = new Response.Builder();
    b.code(newHttpResponse.statusCode());
    b.request(null);
    b.protocol(Protocol.HTTP_1_1);
    Optional<String> contentType = newHttpResponse.headers().firstValue("Content-Type");
    if (contentType.isPresent()) {
      b.body(ResponseBody.create(MediaType.parse(contentType.get()), newHttpResponse.body()));
    }
    for (var e : newHttpResponse.headers().map().entrySet()) {
      String k = e.getKey();
      for (String v : e.getValue()) {
        b.header(k, v);
      }
    }
    return b.build();
  }

  private java.net.http.HttpRequest convertRequest(@NotNull HttpRequest oldHttpRequest)
      throws IOException {
    java.net.http.HttpRequest.BodyPublisher body = getRequestBody(oldHttpRequest);

    var newHttpRequest =
        java.net.http.HttpRequest.newBuilder()
            .method(oldHttpRequest.getMethod(), body)
            .uri(URI.create(oldHttpRequest.getUrl()));
    for (var e : oldHttpRequest.getHeaders().toMultimap().entrySet()) {
      String k = e.getKey();
      for (var v : e.getValue()) {
        newHttpRequest.header(k, v);
      }
    }
    newHttpRequest.setHeader("User-Agent", userAgent.getValue());
    return newHttpRequest.build();
  }

  @NotNull
  private java.net.http.HttpRequest.BodyPublisher getRequestBody(@NotNull HttpRequest httpRequest)
      throws IOException {
    if (httpRequest.getBody() == null) {
      return java.net.http.HttpRequest.BodyPublishers.noBody();
    }
    Buffer b = new Buffer();
    RequestBody body = httpRequest.getBody();
    body.writeTo(b);
    return java.net.http.HttpRequest.BodyPublishers.ofByteArray(b.readByteArray());
  }
}
