package riggit;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.ReplyCommentNode;
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
                  .map(this::setupFeedSubmission)
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

  private Parent setupFeedSubmission(Submission t) {
    var loader = new FXMLLoader();
    loader.setLocation(MainController.class.getResource("/fxml/submission.fxml"));
    loader.setController(new SubmissionController(t, true));
    loader.setClassLoader(MainController.class.getClassLoader());
    try {
      Parent teaserNode = loader.load();
      teaserNode.getStyleClass().add("teaser");
      teaserNode.setOnMouseClicked(e -> setupContentSubmission(t));
      return teaserNode;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void setupContentSubmission(Submission submission) {
    var subLoader = new FXMLLoader();
    subLoader.setLocation(MainController.class.getResource("/fxml/submission.fxml"));
    subLoader.setController(new SubmissionController(submission, false));
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
    this.comments.clear();
    var task =
        new Task<RootCommentNode>() {
          @Override
          protected RootCommentNode call() throws Exception {
            return redditService.getRedditClient().submission(submission.getId()).comments();
          }
        };
    task.setOnSucceeded(e -> updateComments((RootCommentNode) e.getSource().getValue()));
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());

    Thread th = new Thread(task);
    th.setDaemon(true);
    th.start();
  }

  CommentNode rootComment;
  Map<String, CommentController> comments = new HashMap<>();

  public void updateComments(CommentNode rootComment) {
    this.rootComment = rootComment;
    updateComments();
  }

  public void updateComments() {
    Iterator<CommentNode<PublicContribution<?>>> it = rootComment.walkTree().iterator();

    while (it.hasNext()) {
      CommentNode<?> commentNode = it.next();
      if (commentNode instanceof RootCommentNode) {
        // This is the post itself. We don't need that.
      } else if (commentNode instanceof ReplyCommentNode) {
        var replyNode = (ReplyCommentNode) commentNode;
        var replyComment = replyNode.getSubject();
        if (!this.comments.containsKey(replyComment.getFullName())) {
          CommentController commentController =
              CommentController.create(redditService, replyNode, this::loadMoreComments);
          this.comments.put(replyComment.getFullName(), commentController);
          postContent.getChildren().add(commentController.getRoot());
          if (replyNode.getParent() instanceof ReplyCommentNode) {
            var parentFullName = replyNode.getParent().getSubject().getFullName();
            var parentCommentController = this.comments.get(parentFullName);
            parentCommentController.attachChildComment(commentController);
          }
        }
      }
    }
  }

  private void loadMoreComments(CommentController c) {
    Task<Void> task =
        new Task<>() {
          @Override
          protected Void call() throws Exception {
            List<CommentNode<?>> newChildren =
                c.getRootComment().replaceMore(redditService.getRedditClient());
            return null;
          }
        };
    task.setOnSucceeded(e -> updateComments());
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());
    Thread t = new Thread(task);
    t.setDaemon(true);
    t.start();
  }
}
