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
    if ("link".equals(submission.getPostHint())) {
      if ("v.redd.it".equals(submission.getDomain())) {
        contentPane.getChildren().add(new ImageView(submission.getThumbnail()));
      } else {
        contentPane.getChildren().add(new Hyperlink(submission.getUrl()));
      }
    } else if ("image".equals(submission.getPostHint()) && submission.getThumbnail() != null) {
      if (teaser) {
        if (submission.getDomain().equals("i.redd.it")) {

        } else {
          contentPane.getChildren().add(new ImageView(submission.getThumbnail()));
        }
      } else {
        // todo
        contentPane.getChildren().add(new Label("<Video>"));
      }
    } else if ("self".equals(submission.getPostHint())) { // text
      if(teaser) {
        Label text = new Label(submission.getSelfText().substring(0, 200));
        text.setWrapText(true);
        text.setStyle("-fx-max-height: 3.7em; -fx-text-overrun: word-ellipsis;");
        contentPane.getChildren().add(text);
      } else {
        Label text = new Label(submission.getSelfText());
        text.setWrapText(true);
        contentPane.getChildren().add(text);
      }
    } else {
      // e.g. text post without text (title only)
      //contentPane.getChildren().add(new Label("<ELSE>"));
    }
  }
}
