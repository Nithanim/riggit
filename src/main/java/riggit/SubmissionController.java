package riggit;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import net.dean.jraw.models.Submission;

public class SubmissionController implements Initializable {
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
      // e.g. text post without text (title only)
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
  }

  private void handleRichVideo() {
    // e.g. gfycat.com
    contentPane.getChildren().add(new Label("<rich:video>"));
  }

  private void handleHostedVideo() {
    if ("v.redd.it".equals(submission.getDomain())) {}

    contentPane.getChildren().add(new Label("<hosted:video>"));
  }

  private void handleSelf() {
    if (teaser) {
      Label text = new Label(submission.getSelfText().substring(0, 200));
      text.setWrapText(true);
      text.setStyle("-fx-max-height: 3.7em; -fx-text-overrun: word-ellipsis;");
      contentPane.getChildren().add(text);
    } else {
      Label text = new Label(submission.getSelfText());
      text.setWrapText(true);
      contentPane.getChildren().add(text);
    }
  }

  private void handleImage() {
    if (submission.getThumbnail() != null) {
      contentPane.getChildren().add(new Label("<Image without thumbnail>"));
    }
    if (teaser) {
      if (submission.getDomain().equals("i.redd.it")) {
        contentPane.getChildren().add(new ImageView(submission.getThumbnail()));
      } else {
        contentPane.getChildren().add(new ImageView(submission.getThumbnail()));
      }
    } else {

      if (submission.getDomain().equals("i.redd.it")) {
        contentPane.getChildren().add(new ImageView(submission.getUrl()));
      } else {
        // todo
        contentPane.getChildren().add(new Label("<Image>"));
      }
    }
  }

  private void handleLink() {
    if ("v.redd.it".equals(submission.getDomain())) {
      contentPane.getChildren().add(new ImageView(submission.getThumbnail()));
    } else if ("i.imgur.com".equals(submission.getDomain())) {
      if (teaser) {
        contentPane.getChildren().add(new ImageView(submission.getThumbnail()));
      } else {
        if (submission.getUrl().endsWith(".gifv")) {
          contentPane.getChildren().add(new Label("<imgur:video>"));
        } else {
          contentPane.getChildren().add(new ImageView(submission.getUrl()));
        }
      }
    } else {
      contentPane.getChildren().add(new Hyperlink(submission.getUrl()));
    }
  }
}
