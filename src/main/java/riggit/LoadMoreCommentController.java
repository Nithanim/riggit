package riggit;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;

public class LoadMoreCommentController implements Initializable {
  @SneakyThrows
  public static LoadMoreCommentController create(RedditService redditService, CommentNode<?> rootComment) {
    var controller = new LoadMoreCommentController(redditService, rootComment);
    controller.initialize(null, null);
    return controller;
  }

  private @Getter
  final VBox root;
  private final Button loadMoreButton;

  private final RedditService redditService;
  private final CommentNode<?> rootComment;
  private final List<String> unloadedCommentIds;

  public LoadMoreCommentController(RedditService redditService, CommentNode<?> rootComment) {
    this.redditService = redditService;
    this.rootComment = rootComment;
    this.root = new VBox();
    this.loadMoreButton = new Button();
    this.unloadedCommentIds = new ArrayList<>(rootComment.getMoreChildren().getChildrenIds());
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    loadMoreButton.setText("Load more comments...");
    loadMoreButton.setOnAction(e -> loadComments());
    root.getChildren().add(loadMoreButton);
  }

  private void loadComments() {
    String idToLoad = unloadedCommentIds.get(0);
    var task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        var comment = redditService.getRedditClient().comment(idToLoad);
        return null;
      }
    };
    task.setOnFailed(e -> e.getSource().getException().printStackTrace());
    task.setOnSucceeded(
            e -> {

            });
    Thread t = new Thread(task);
    t.start();
  }

}
