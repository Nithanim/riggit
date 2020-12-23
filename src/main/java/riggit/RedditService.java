package riggit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.SneakyThrows;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Account;
import net.dean.jraw.oauth.AccountHelper;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.JsonFileTokenStore;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.oauth.StatefulAuthHelper;
import okhttp3.OkHttpClient;

public class RedditService implements AutoCloseable {
  private final Path loginFile = Paths.get("login.json");
  private final Path settingsFile = Paths.get("settings.json");

  private final AccountHelper accountHelper;
  private final Settings settings;

  @SneakyThrows
  public RedditService() throws IOException {
    UserAgent userAgent = new UserAgent("desktop", "riggit", "0.0.1", "nithanim");
    // var credentials = Credentials.installedApp("-2Yve2Ar8e_SzQ", "http://127.0.0.1:8070/");
    var credentials = Credentials.installedApp("QZAJpMpbMuAlwQ", "http://127.0.0.1:8070/");

    if (!Files.exists(loginFile)) {
      Files.writeString(loginFile, "{}");
    }
    JsonFileTokenStore tokenStore = new JsonFileTokenStore(loginFile.toFile(), Map.of());
    tokenStore.load();
    settings = Objects.requireNonNullElse(Settings.load(settingsFile), new Settings());

    if (settings.getUuid() == null) {
      settings.setUuid(UUID.randomUUID());
    }

    NetworkAdapter networkAdapter = new OkHttpNetworkAdapter(userAgent);
    StatefulAuthHelper authHelper =
        OAuthHelper.interactive(networkAdapter, credentials, tokenStore);

    RedditClient reddit;

    AccountHelper accountHelper =
        new AccountHelper(networkAdapter, credentials, tokenStore, settings.uuid);
    if (settings.getUsername() == null) {
      reddit = accountHelper.switchToUserless();
    } else {
      var switchTry = accountHelper.trySwitchToUser(settings.getUsername());

      if (switchTry != null) {
        reddit = switchTry;
      } else {
        String authUrl =
            authHelper.getAuthorizationUrl(true, false, "read", "vote", "flair", "identity");

        System.out.println("Go to: " + authUrl);

        AuthHttpServer authServer = new AuthHttpServer();
        String returnUrl = authServer.waitForAuth().get();
        Thread.sleep(200);
        authServer.stop();

        reddit = authHelper.onUserChallenge(returnUrl);
        Account me = reddit.me().query().getAccount();

        settings.setUsername(me.getName());
      }
    }

    this.accountHelper = accountHelper;
  }

  public RedditClient getRedditClient() {
    return accountHelper.getReddit();
  }

  @SneakyThrows
  @Override
  public void close() {
    close(accountHelper.getReddit());
    Settings.persist(settings, settingsFile);
  }

  /** (IntelliJ only?) fix for non-daemon connection pool. */
  @SneakyThrows
  private static void close(RedditClient reddit) {
    Field f = OkHttpNetworkAdapter.class.getDeclaredField("http");
    f.setAccessible(true);
    Object o = f.get(reddit.getHttp());
    ((OkHttpClient) o).connectionPool().evictAll();
  }
}
