package riggit;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import net.dean.jraw.models.Submission;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.ReplyCommentNode;
import net.dean.jraw.tree.RootCommentNode;

public class MainController implements Initializable {
  @FXML VBox postFeed;
  @FXML VBox postContent;

  private RedditService redditService = new RedditService();
  /** Hold strong reference (e.g. for media player) */
  private SubmissionController submissionController;

  public MainController() throws IOException {}

  @SneakyThrows
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    new FeedController(redditService, this::setupContentSubmission, postFeed);

    registerFocusListener(this::onFocusChanged);
  }

  private void registerFocusListener(ChangeListener<Boolean> focusListener) {
    postContent
        .sceneProperty()
        .addListener(
            (observableValue, a, b) -> {
              if (a != null) {
                a.getWindow().focusedProperty().removeListener(focusListener);
              }
              b.getWindow()
                  .focusedProperty()
                  .removeListener(
                      focusListener); // Needed because... this listener is called multiple times
              // with a=null and the same stage
              b.getWindow().focusedProperty().addListener(focusListener);
            });
  }

  private void onFocusChanged(
      ObservableValue<? extends Boolean> observableValue, Boolean a, Boolean b) {
    if (b) {
      String text = (String) Clipboard.getSystemClipboard().getContent(DataFormat.PLAIN_TEXT);
      if (text != null) {
        Matcher m =
            Pattern.compile(
                    "https://www\\.reddit\\.com/r/([^/]+)/comments/([^/]+)/([^/]+)/(?:\\?.*|$)")
                .matcher(text);
        if (m.matches()) {
          String submissionId = m.group(2);
          if (submissionController == null
              || !submissionController.getSubmission().getId().equals(submissionId)) {
            var submissionReference = redditService.getRedditClient().submission(submissionId);
            setupContentSubmission(submissionReference.inspect());
          }
        }
      }
    }
  }

  public void postInitialize() {
    ((Stage) postFeed.getScene().getWindow()).setOnCloseRequest((e) -> redditService.close());
  }

  private void setupContentSubmission(Submission submission) {
    var subLoader = new FXMLLoader();
    subLoader.setLocation(MainController.class.getResource("/fxml/submission.fxml"));
    submissionController = new SubmissionController(submission, false);
    subLoader.setController(submissionController);
    subLoader.setClassLoader(MainController.class.getClassLoader());
    try {
      postContent.getChildren().clear();
      postContent.getChildren().add(subLoader.load());

      setupComments(submission);

    } catch (IOException e2) {
      throw new IllegalStateException(e2);
    }
  }

  private void setupComments(Submission submission) {
    this.controllerMap.clear();
    var task =
        new Task<RootCommentNode>() {
          @Override
          protected RootCommentNode call() throws Exception {
            return redditService.getRedditClient().submission(submission.getId()).comments();
          }
        };
    task.setOnSucceeded(e -> setupComments((RootCommentNode) e.getSource().getValue()));
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());

    Thread th = new Thread(task);
    th.setDaemon(true);
    th.start();
  }

  CommentNode rootComment;
  Map<String, CommentController> controllerMap = new HashMap<>();

  public void setupComments(CommentNode rootComment) {
    this.rootComment = rootComment;
    for (var commentNode : rootComment.getReplies()) {
      var replyNode = (ReplyCommentNode) commentNode;
      var replyComment = replyNode.getSubject();
      CommentController commentController =
          CommentController.create(redditService, replyNode, this::loadMoreComments);
      this.controllerMap.put(replyComment.getFullName(), commentController);
      commentController.update();
      postContent.getChildren().add(commentController.getRoot());
    }
  }

  private void loadMoreComments(CommentController c) {
    var task =
        new Task<>() {
          @Override
          protected List<CommentNode<?>> call() throws Exception {
            return c.getRootComment().replaceMore(redditService.getRedditClient());
          }
        };
    // task.setOnSucceeded(e -> setupComments());
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());
    Thread t = new Thread(task);
    t.setDaemon(true);
    t.start();
  }
}
