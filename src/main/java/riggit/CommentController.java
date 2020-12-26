package riggit;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.ReplyCommentNode;

public class CommentController implements Initializable {
  @SneakyThrows
  public static CommentController create(
      RedditService redditService,
      CommentNode<?> rootComment,
      Consumer<CommentController> loadMoreCommentsCallback) {
    var loader = new FXMLLoader();
    var controller = new CommentController(redditService, rootComment);
    loader.setController(controller);
    loader.setLocation(MainController.class.getResource("/fxml/comment.fxml"));
    loader.setClassLoader(MainController.class.getClassLoader());
    loader.load();
    return controller;
  }

  @FXML @Getter Parent root;
  @FXML Label username;
  @FXML VBox contentPane;
  @FXML VBox subcomments;
  @FXML Label scoreNumber;
  @FXML Label date;

  private final RedditService redditService;
  @Getter private final CommentNode<?> rootComment;
  private final Button loadMoreButton;

  private Map<String, CommentController> existing = new HashMap<>();

  public CommentController(RedditService redditService, CommentNode<?> rootComment) {
    this.redditService = redditService;
    this.rootComment = rootComment;
    if (rootComment.hasMoreChildren()) {
      loadMoreButton = new Button();
      loadMoreButton.setText(" more comments...");
    } else {
      loadMoreButton = null;
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    if (rootComment instanceof ReplyCommentNode) {
      ReplyCommentNode c = (ReplyCommentNode) rootComment;
      Comment s = c.getSubject();
      username.setText(s.getAuthor());
      scoreNumber.setText(String.valueOf(s.getScore()));
      date.setText(DateUtil.dateToDifference(DateUtil.convertToLocalDateTime(s.getCreated())));

      Label text = new Label(s.getBody());
      text.setWrapText(true);
      text.setMinHeight(Region.USE_PREF_SIZE); // This (somehow) makes text wrap work...
      contentPane.getChildren().add(text);

      if (rootComment.hasMoreChildren()) {
        // TODO thread continuation
        // Number of comments to load is wrong :(
        loadMoreButton.setText(
            rootComment.getMoreChildren().getChildrenIds().size() + " " + loadMoreButton.getText());
        subcomments.getChildren().add(loadMoreButton);
        loadMoreButton.setOnAction(e -> onLoadMoreComments());
      }
    }
  }

  private void onLoadMoreComments() {
    var task =
        new Task<>() {
          @Override
          protected List<CommentNode<?>> call() throws Exception {
            return rootComment.replaceMore(redditService.getRedditClient());
          }
        };
    task.setOnSucceeded(e -> update(true));
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());
    Thread t = new Thread(task);
    t.setDaemon(true);
    t.start();
  }

  public void update() {
    update(false);
  }

  private void update(boolean loadMore) {
    var replies = rootComment.getReplies();
    var allReplies = new LinkedHashMap<String, CommentNode<Comment>>();
    for (var reply : replies) {
      allReplies.put(reply.getSubject().getFullName(), reply);
    }
    Map<String, CommentNode<Comment>> missingReplies = new LinkedHashMap<>(allReplies);

    missingReplies.keySet().removeAll(existing.keySet());

    if (!missingReplies.isEmpty()) {
      removeButton();

      for (var e : missingReplies.entrySet()) {
        CommentController commentController =
            CommentController.create(redditService, e.getValue(), null);
        subcomments.getChildren().add(commentController.getRoot());
        existing.put(e.getKey(), commentController);
      }
    }
    addButtonIfNecessary();
    existing.values().forEach(CommentController::update);
  }

  private void addButtonIfNecessary() {
    if (rootComment.hasMoreChildren()) {
      if (subcomments.getChildren().isEmpty()
          || subcomments.getChildren().get(subcomments.getChildren().size() - 1)
              != loadMoreButton) {
        subcomments.getChildren().add(loadMoreButton);
      }
    }
  }

  private void removeButton() {
    if (!subcomments.getChildren().isEmpty()) {
      if (subcomments.getChildren().get(subcomments.getChildren().size() - 1) == loadMoreButton) {
        subcomments.getChildren().remove(subcomments.getChildren().size() - 1);
      }
    }
  }
}
