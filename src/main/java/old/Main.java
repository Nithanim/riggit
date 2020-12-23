package old;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.*;
import lombok.SneakyThrows;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Account;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.oauth.AccountHelper;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.JsonFileTokenStore;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.oauth.StatefulAuthHelper;
import net.dean.jraw.pagination.DefaultPaginator;
import okhttp3.OkHttpClient;
import riggit.AuthHttpServer;
import riggit.Settings;

public class Main {
  public static void main(String[] args)
      throws IOException, ExecutionException, InterruptedException {
    UserAgent userAgent = new UserAgent("desktop", "riggit", "0.0.1", "nithanim");
    // var credentials = Credentials.installedApp("-2Yve2Ar8e_SzQ", "http://127.0.0.1:8070/");
    var credentials = Credentials.installedApp("QZAJpMpbMuAlwQ", "http://127.0.0.1:8070/");

    Path loginFile = Paths.get("login.json");
    Path settingsFile = Paths.get("settings.json");
    if (!Files.exists(loginFile)) {
      Files.writeString(loginFile, "{}");
    }
    JsonFileTokenStore tokenStore = new JsonFileTokenStore(loginFile.toFile(), Map.of());
    tokenStore.load();
    Settings settings = Objects.requireNonNullElse(Settings.load(settingsFile), new Settings());

    if (settings.getUuid() == null) {
      settings.setUuid(UUID.randomUUID());
    }

    NetworkAdapter networkAdapter = new OkHttpNetworkAdapter(userAgent);
    StatefulAuthHelper authHelper =
        OAuthHelper.interactive(networkAdapter, credentials, tokenStore);

    RedditClient reddit;

    AccountHelper helper =
        new AccountHelper(networkAdapter, credentials, tokenStore, settings.getUuid());
    if (settings.getUsername() == null) {
      reddit = helper.switchToUserless();
    } else {
      var switchTry = helper.trySwitchToUser(settings.getUsername());

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

    DefaultPaginator<Submission> frontpage =
        reddit.frontPage().limit(10).sorting(SubredditSort.HOT).build();
    frontpage.next();

    LafManager.install(new DarculaTheme());
    JFrame frame = new JFrame("Riggit");
    MainFrame mainFrame = new MainFrame();

    DefaultListModel<PostTeaser> lm = new DefaultListModel<>();
    lm.addAll(
        frontpage.getCurrent().stream()
            .map(e -> new PostTeaser(e.getId(), e.getTitle(), e.getSelfText(), e.getSubreddit()))
            .collect(Collectors.toList()));
    mainFrame.getPostsList().setModel(lm);

    frame.setContentPane(mainFrame.getPanel1());
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);

    close(reddit);
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
