package riggit;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;

public class MainController implements Initializable {
  @FXML VBox postFeed;
  @FXML VBox postContent;

  private RedditService redditService = new RedditService();

  public MainController() throws IOException {}

  @SneakyThrows
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    Callable<List<Parent>> c =
        () -> {
          RedditClient reddit = redditService.getRedditClient();
          DefaultPaginator<Submission> frontpage =
              reddit.frontPage().limit(20).sorting(SubredditSort.HOT).build();
          frontpage.next();

          List<Parent> teaserNodes =
              frontpage.getCurrent().stream()
                  .map(this::createFeedSubmission)
                  .collect(Collectors.toList());
          return teaserNodes;
        };
    var task =
        new Task<List<Parent>>() {
          @Override
          protected List<Parent> call() throws Exception {
            return c.call();
          }
        };
    task.setOnSucceeded(
        e -> {
          List<Parent> value = (List<Parent>) e.getSource().getValue();
          postFeed.getChildren().addAll(value);
        });
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());

    Thread th = new Thread(task);
    th.setDaemon(true);
    th.start();
  }

  public void postInitialize() {
    ((Stage) postFeed.getScene().getWindow()).setOnCloseRequest((e) -> redditService.close());
  }

  private Parent createFeedSubmission(Submission t) {
    var loader = new FXMLLoader();
    loader.setLocation(MainController.class.getResource("/fxml/submission.fxml"));
    loader.setController(new SubmissionController(t, true));
    loader.setClassLoader(MainController.class.getClassLoader());
    try {
      Parent teaserNode = loader.load();
      teaserNode.getStyleClass().add("teaser");
      teaserNode.setOnMouseClicked(e -> createContentSubmission(t));
      return teaserNode;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void createContentSubmission(Submission submission) {
    var subLoader = new FXMLLoader();
    subLoader.setLocation(MainController.class.getResource("/fxml/submission.fxml"));
    subLoader.setController(new SubmissionController(submission, false));
    subLoader.setClassLoader(MainController.class.getClassLoader());
    try {
      postContent.getChildren().clear();
      postContent.getChildren().add(subLoader.load());

      loadComments(submission);

    } catch (IOException e2) {
      throw new IllegalStateException(e2);
    }
  }

  private void loadComments(Submission submission) {
    var task =
        new Task<RootCommentNode>() {
          @Override
          protected RootCommentNode call() throws Exception {
            return redditService.getRedditClient().submission(submission.getId()).comments();
          }
        };
    task.setOnSucceeded(
        e -> {
          for (CommentNode<?> comment : (RootCommentNode) e.getSource().getValue()) {
            postContent.getChildren().add(CommentController.create(redditService, comment).getRoot());
          }
        });
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());

    Thread th = new Thread(task);
    th.setDaemon(true);
    th.start();
  }
}
