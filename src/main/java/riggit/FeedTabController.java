package riggit;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;

public class FeedTabController implements Initializable {
  private final TabPane feedTabs;
  private final FeedController feedController;

  private Tab newSubredditTab;

  public FeedTabController(TabPane feedTabs, FeedController feedController) {
    this.feedTabs = feedTabs;
    this.feedController = feedController;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    newSubredditTab = new Tab("+");
    newSubredditTab.setId(newSubredditTab.getText());
    feedTabs.getSelectionModel().selectedItemProperty().addListener(new TabChangeListeners());

    Tab frontpageTab = new Tab("frontpage");
    frontpageTab.setId(frontpageTab.getText());
    feedTabs.getTabs().add(frontpageTab);
    feedTabs.getTabs().add(newSubredditTab);
  }

  private class TabChangeListeners implements ChangeListener<Tab> {
    String currentSubreddit;

    @Override
    public void changed(ObservableValue<? extends Tab> observableValue, Tab oldTab, Tab newTab) {
      if (newTab != newSubredditTab) {
        if (!newTab.getId().equals(currentSubreddit)) {
          currentSubreddit = newTab.getId();
          feedController.switchSubreddit(newTab.getId());
        }
      } else {
        String subredditName = askForSubredditName();
        if (subredditName != null) {
          insertNewTabAtEnd(subredditName);
        } else {
          selectTab(oldTab);
        }
      }
    }

    private void insertNewTabAtEnd(String subredditName) {
      var allTabs = feedTabs.getTabs();
      allTabs.remove(newSubredditTab);
      Tab tab = new Tab(subredditName);
      tab.setId(tab.getText());
      allTabs.add(tab);
      feedTabs.getSelectionModel().select(tab);
      allTabs.add(newSubredditTab);

      ContextMenu cm = new ContextMenu();
      CheckMenuItem cmi = new CheckMenuItem("Remove");
      cmi.setOnAction(e -> allTabs.remove(tab));
      cm.getItems().add(cmi);
      tab.setContextMenu(cm);
    }

    private void selectTab(Tab tab) {
      feedTabs.getSelectionModel().select(tab);
    }

    private String askForSubredditName() {
      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("Add subreddit");
      dialog.setHeaderText("Add a new subreddit as a tab");
      dialog.setContentText("Please enter the subreddit name:");

      return dialog.showAndWait().orElse(null);
    }
  }
}
