package riggit;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {
  @Override
  public void start(Stage primaryStage) throws IOException {
    FXMLLoader loader = new FXMLLoader();
    loader.setClassLoader(MainFx.class.getClassLoader());
    loader.setLocation(MainFx.class.getResource("/fxml/main.fxml"));
    MainController mainController = new MainController();
    loader.setController(mainController);
    Parent component = loader.load();

    Scene scene = new Scene(component, 1024, 768);
    scene.getStylesheets().add("/fxml/dark.css");

    primaryStage.setTitle("Riggit");
    primaryStage.setScene(scene);
    primaryStage.show();
    mainController.postInitialize();
  }
}
