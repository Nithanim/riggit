package riggit;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AuthHttpServer extends NanoHTTPD {

  private CompletableFuture<String> future;

  public AuthHttpServer() throws IOException {
    super(8070);
  }

  public CompletableFuture<String> waitForAuth() throws IOException {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    return this.future = new CompletableFuture<String>();
  }

  @Override
  public Response serve(IHTTPSession session) {
    future.complete(
        "http://"
            + session.getHeaders().get("host")
            + session.getUri()
            + "?"
            + session.getQueryParameterString());
    return newFixedLengthResponse(
        "<html><body><h1>Go back to the app!</h1>\n<script>window.setTimeout(3000, function() {window.close();});</script></body></html>\n");
  }
}
