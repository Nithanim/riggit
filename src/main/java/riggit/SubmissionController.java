package riggit;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.dean.jraw.models.Submission;

public class SubmissionController implements Initializable {
  @FXML Pane root;
  @FXML VBox contentPane;
  @FXML Label title;
  @FXML Label subreddit;
  @FXML Parent textPane;
  @FXML Label username;
  @FXML Label scoreNumber;
  @FXML Label commentsNumber;
  @FXML Label date;

  private final Submission submission;
  private final boolean teaser;

  public SubmissionController(Submission submission, boolean teaser) {
    this.submission = submission;
    this.teaser = teaser;
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    title.setText(submission.getTitle());
    subreddit.setText("r/" + submission.getSubreddit());
    username.setText("u/" + submission.getAuthor());
    commentsNumber.setText(
        submission.getCommentCount() == null ? "?" : String.valueOf(submission.getCommentCount()));
    scoreNumber.setText(submission.isScoreHidden() ? "?" : String.valueOf(submission.getScore()));
    LocalDateTime creationDate = DateUtil.convertToLocalDateTime(submission.getCreated());
    date.setText(DateUtil.dateToDifference(creationDate));
    date.setTooltip(new Tooltip(creationDate.withNano(0).toString()));
    // null allowed
    if (submission.getPostHint() == null) {
      // e.g. text post without text (title only) BUT NOT ALWAYS
      if(submission.getSelfText() != null) {
        handleSelf();
      }
    } else if ("link".equals(submission.getPostHint())) {
      handleLink();
    } else if ("image".equals(submission.getPostHint())) {
      handleImage();
    } else if ("self".equals(submission.getPostHint())) { // text
      handleSelf();
    } else if ("hosted:video".equals(submission.getPostHint())) {
      handleHostedVideo();
    } else if ("rich:video".equals(submission.getPostHint())) {
      handleRichVideo();
    } else {
      contentPane.getChildren().add(new Label("<ELSE>"));
    }

    ContextMenu contextMenu = new ContextMenu();
    MenuItem menuItem1 = new MenuItem("Copy link");
    menuItem1.setOnAction(
        e -> {
          final ClipboardContent clipboardContent = new ClipboardContent();
          clipboardContent.putString("https://reddit.com" + submission.getPermalink());
          Clipboard.getSystemClipboard().setContent(clipboardContent);
        });
    contextMenu.getItems().add(menuItem1);

    root.setOnMousePressed(
        e -> {
          if (e.getButton() == MouseButton.SECONDARY) {
            contextMenu.show(root, e.getScreenX(), e.getScreenY());
          }
        });
  }

  private void handleRichVideo() {
    // e.g. gfycat.com; https://youtu.be/4et-mghfdfdf--
    if (teaser) {
      contentPane.getChildren().add(makeVideoThumbnail(submission.getThumbnail()));
    } else {
      if ("youtu.be".equals(submission.getDomain())
          || "youtube.com".equals(submission.getDomain())) {
        contentPane.getChildren().add(new Hyperlink(submission.getUrl()));
      } else {
        contentPane.getChildren().add(new Label("<rich:video>"));
      }
    }
  }

  private void handleHostedVideo() {
    if ("v.redd.it".equals(submission.getDomain())) {
      if (teaser) {
        if ("default".equals(submission.getThumbnail())) {
          // e.g. https://v.redd.it/wh30vduq4y761
          contentPane.getChildren().add(new Label("<vreddit without preview>"));
        } else if ("nsfw".equals(submission.getThumbnail())) {
          contentPane.getChildren().add(new Label("<vreddit nsfw>"));
        } else {
          contentPane.getChildren().add(makeVideoThumbnail(submission.getThumbnail()));
        }
      } else {
        String thumbnail = "nsfw".equals(submission.getThumbnail()) ? null : submission.getThumbnail();
        VideoPlayer vp =
            new VideoPlayer(thumbnail, submission.getUrl() + "/DASHPlaylist.mpd");
        contentPane.getChildren().add(vp.getRoot());
      }
    } else {
      contentPane.getChildren().add(new Label("<hosted:video>"));
    }
  }

  private void handleSelf() {
    if (teaser) {
      String text = submission.getSelfText();
      var label = new Label(text.substring(0, Math.min(text.length(), 200)));
      label.setWrapText(true);
      label.setStyle("-fx-max-height: 3.7em; -fx-text-overrun: word-ellipsis;");
      contentPane.getChildren().add(label);
    } else {
      Label text = new Label(submission.getSelfText());
      text.setWrapText(true);
      contentPane.getChildren().add(text);
    }
  }

  private void handleImage() {
    if (submission.getThumbnail() == null) {
      contentPane.getChildren().add(new Label("<Image without thumbnail>"));
    }
    if (teaser) {
      if (submission.getDomain().equals("i.redd.it")) {
        if ("image".equals(submission.getThumbnail())) {
          // why? maybe size too small for thumbnail
          contentPane.getChildren().add(makeResizableImageView(submission.getUrl()));
        } else if ("nsfw".equals(submission.getThumbnail())) {
          contentPane.getChildren().add(new Label("nsfw"));
        } else {
          contentPane.getChildren().add(makeResizableImageView(submission.getThumbnail()));
        }
      } else {
        contentPane.getChildren().add(makeResizableImageView(submission.getThumbnail()));
      }
    } else {
      if (submission.getDomain().equals("i.redd.it")
          || submission.getDomain().equals("i.imgur.com")) {
        contentPane.getChildren().add(makeResizableImageView(submission.getUrl()));
      } else {
        // todo
        contentPane.getChildren().add(new Label("<Image>"));
      }
    }
  }

  private void handleLink() {
    if ("nsfw".equals(submission.getThumbnail())) {
      contentPane.getChildren().add(new Label("<link:nsfw>"));
    } else if ("v.redd.it".equals(submission.getDomain())) {
      contentPane.getChildren().add(makeResizableImageView(submission.getThumbnail()));
    } else if ("i.imgur.com".equals(submission.getDomain())
        || "imgur.com".equals(submission.getDomain())) {
      if (teaser) {
        if (submission.getUrl().endsWith(".gifv")) {
          /*VideoPlayer vp =
                  new VideoPlayer(submission.getThumbnail(), submission.getUrl() + "/HLSPlaylist.m3u8");
          contentPane.getChildren().add(vp.getRoot());*/
        } else {
          contentPane.getChildren().add(new Label("TODO gifv"));
          contentPane.getChildren().add(makeResizableImageView(submission.getThumbnail()));
        }
      } else {
        if (submission.getUrl().endsWith(".gifv")) {
          contentPane.getChildren().add(new Label("<imgur:video>"));
        } else {
          contentPane.getChildren().add(makeResizableImageView(submission.getUrl()));
        }
      }
    } else {
      contentPane.getChildren().add(new Hyperlink(submission.getUrl()));
    }
  }

  private Pane makeVideoThumbnail(String url) {
    var stackPane = new StackPane();
    stackPane.getChildren().add(new ImageView(url));
    Label tag = new Label("VIDEO");
    tag.setStyle("-fx-stroke: black; -fx-stroke-width: 1; -fx-fill: white;");
    stackPane.getChildren().add(tag);
    StackPane.setAlignment(tag, Pos.BOTTOM_LEFT);
    stackPane.maxHeight(Region.USE_COMPUTED_SIZE);
    stackPane.setMaxWidth(Region.USE_COMPUTED_SIZE);
    var borderPane = new BorderPane();
    borderPane.setCenter(stackPane);
    return borderPane;
  }

  private BorderPane makeResizableImageView(String url) {
    BorderPane borderPane = new BorderPane();
    ImageView iv = new ResizableImageView(url);
    iv.setPreserveRatio(true);
    borderPane.setCenter(iv);
    return borderPane;
  }
}
