package riggit;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.pagination.DefaultPaginator;

public class FeedController {
  private final RedditService redditService;
  private final Consumer<Submission> setupContentSubmission;
  private final VBox postFeed;
  private final Button loadMoreItemsButton;

  private DefaultPaginator<Submission> paginator;

  public FeedController(
      RedditService redditService, Consumer<Submission> setupContentSubmission, VBox postFeed) {
    this.redditService = redditService;
    this.setupContentSubmission = setupContentSubmission;
    this.postFeed = postFeed;

    this.loadMoreItemsButton = new Button();
    loadMoreItemsButton.setMaxWidth(Double.MAX_VALUE);
    loadMoreItemsButton.setOnAction(e -> doLoadMore());
  }

  public void switchSubreddit(String subredditName) {
    RedditClient reddit = redditService.getRedditClient();
    paginator =
        reddit.subreddit(subredditName).posts().limit(20).sorting(SubredditSort.HOT).build();

    postFeed.getChildren().clear();
    postFeed.getChildren().add(loadMoreItemsButton);
    onLoadSuccessful(List.of());
    doLoadMore();
  }

  private void doLoadMore() {
    onLoadStart();
    var task =
        new Task<List<Parent>>() {
          @Override
          protected List<Parent> call() throws Exception {
            List<Parent> teaserNodes =
                paginator.next().stream()
                    .map(FeedController.this::setupFeedSubmission)
                    .collect(Collectors.toList());
            return teaserNodes;
          }
        };
    task.setOnSucceeded(e -> onLoadSuccessful((List<Parent>) e.getSource().getValue()));
    task.setOnFailed(e -> onLoadFailed(e.getSource().getException()));

    Thread th = new Thread(task);
    th.setDaemon(true);
    th.start();
  }

  private void onLoadStart() {
    loadMoreItemsButton.setText("Loading more...");
    if (postFeed.getChildren().size() >= 2) {
      postFeed.getChildren().get(postFeed.getChildren().size() - 2).requestFocus();
    }
    loadMoreItemsButton.setDisable(true);
  }

  private void onLoadFailed(Throwable exception) {
    loadMoreItemsButton.setText("Loading failed! Try again?");
    loadMoreItemsButton.setDisable(false);
    exception.printStackTrace();
  }

  private void onLoadSuccessful(List<Parent> loadedSubmissionsTeasers) {
    if (postFeed.getChildren().size() >= 2) {
      postFeed.getChildren().get(postFeed.getChildren().size() - 2).requestFocus();
    }
    postFeed.getChildren().remove(postFeed.getChildren().size() - 1);
    postFeed.getChildren().addAll(loadedSubmissionsTeasers);
    loadMoreItemsButton.setText("Load more...");
    loadMoreItemsButton.setDisable(false);
    postFeed.getChildren().add(loadMoreItemsButton);
  }

  private Parent setupFeedSubmission(Submission t) {
    var loader = new FXMLLoader();
    loader.setLocation(MainController.class.getResource("/fxml/submission.fxml"));
    loader.setController(new SubmissionController(t, true));
    loader.setClassLoader(MainController.class.getClassLoader());
    try {
      Parent teaserNode = loader.load();
      teaserNode.getStyleClass().add("teaser");
      teaserNode.setOnMouseClicked(e -> onSubmissionClicked(e, t));
      return teaserNode;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void onSubmissionClicked(MouseEvent e, Submission s) {
    if (e.getButton() == MouseButton.PRIMARY) {
      setupContentSubmission.accept(s);
    }
  }
}
