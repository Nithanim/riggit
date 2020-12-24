package riggit;

import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
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
    var controller = new CommentController(redditService, rootComment, loadMoreCommentsCallback);
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
  private final Consumer<CommentController> loadMoreCommentsCallback;
  private final Button loadMoreButton;

  private Set<String> existingIds = new HashSet<>();

  public CommentController(
      RedditService redditService,
      CommentNode<?> rootComment,
      Consumer<CommentController> loadMoreCommentsCallback) {
    this.redditService = redditService;
    this.rootComment = rootComment;
    this.loadMoreCommentsCallback = loadMoreCommentsCallback;
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
        loadMoreButton.setText(
            rootComment.getMoreChildren().getChildrenIds().size() + " " + loadMoreButton.getText());
        subcomments.getChildren().add(loadMoreButton);
        loadMoreButton.setOnAction(e -> loadMoreCommentsCallback.accept(this));
      }
    }
  }

  public void attachChildComment(CommentController commentController) {
    var children = subcomments.getChildren();
    if (children.size() > 0) {
      if (children.get(children.size() - 1) == loadMoreButton) {
        children.add(children.size() - 1, commentController.getRoot());
      } else {
        children.add(commentController.getRoot());
      }
    } else {
      children.add(commentController.getRoot());
    }
  }
}
