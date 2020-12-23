package riggit;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
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
  public static CommentController create(RedditService redditService, CommentNode<?> rootComment) {
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
  private final CommentNode<?> rootComment;

  public CommentController(RedditService redditService, CommentNode<?> rootComment) {
    this.redditService = redditService;
    this.rootComment = rootComment;
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

      var mc = c.getReplies();
      subcomments
          .getChildren()
          .addAll(
              mc.stream()
                  .map(comment -> create(redditService, comment))
                  .map(CommentController::getRoot)
                  .collect(Collectors.toList()));

      if(rootComment.hasMoreChildren()) {
        LoadMoreCommentController loadMoreController = LoadMoreCommentController.create(redditService, rootComment);
        subcomments.getChildren().add(loadMoreController.getRoot());
      }
    }
  }
}
