package riggit;

import java.net.URL;
import java.time.Duration;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import lombok.Getter;
import lombok.SneakyThrows;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class VideoPlayerControlsController implements Initializable {
  @Getter @FXML Parent root;
  @FXML Slider progress;
  @FXML Button stateButton;
  @FXML Label timeCurrent;
  @FXML Label timeTotal;

  private final EmbeddedMediaPlayer mediaPlayer;
  private boolean sliderValueCurrentlyPlayerControlled = false;

  @SneakyThrows
  public static VideoPlayerControlsController create(EmbeddedMediaPlayer mediaPlayer) {
    var loader = new FXMLLoader();
    var controller = new VideoPlayerControlsController(mediaPlayer);
    loader.setController(controller);
    loader.setLocation(
        VideoPlayerControlsController.class.getResource("/fxml/videoplayer_controls.fxml"));
    loader.setClassLoader(VideoPlayerControlsController.class.getClassLoader());
    loader.load();
    controller.root = loader.getRoot();
    return controller;
  }

  public VideoPlayerControlsController(EmbeddedMediaPlayer mediaPlayer) {
    this.mediaPlayer = mediaPlayer;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    progress.setMin(0);
    this.mediaPlayer.events().addMediaPlayerEventListener(new MyMediaPlayerEventAdapter());
    updateTotalTime(this.mediaPlayer.media().info().duration());
    this.stateButton.setOnAction(
        e -> {
          State state = mediaPlayer.media().info().state();
          if (state == State.PAUSED) {
            mediaPlayer.controls().play();
          } else if (state == State.PLAYING) {
            mediaPlayer.controls().pause();
          }
        });
    progress
        .valueProperty()
        .addListener(
            (observableValue, oldValue, newValue) -> {
              if (!sliderValueCurrentlyPlayerControlled) {
                this.mediaPlayer.controls().setTime(newValue.longValue());
              }
            });
  }

  private class MyMediaPlayerEventAdapter extends MediaPlayerEventAdapter {
    @Override
    public void playing(MediaPlayer mediaPlayer) {
      Platform.runLater(VideoPlayerControlsController.this::updateStateButton);
    }

    @Override
    public void paused(MediaPlayer mediaPlayer) {
      Platform.runLater(VideoPlayerControlsController.this::updateStateButton);
    }

    @Override
    public void stopped(MediaPlayer mediaPlayer) {
      Platform.runLater(VideoPlayerControlsController.this::updateStateButton);
    }

    @Override
    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
      updateCurrentTime(newTime);
    }

    @Override
    public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
      updateTotalTime(newLength);
    }
  }

  private void updateStateButton() {
    State state = mediaPlayer.media().info().state();
    switch (state) {
      case PLAYING:
        stateButton.setText("||");
        break;
      case PAUSED:
        stateButton.setText("▶");
        break;
      case ENDED:
        // does not really exist?
      case STOPPED:
        stateButton.setText("⅁");
        break;
      default:
        stateButton.setText("<" + state.name() + ">");
    }
  }

  private void updateCurrentTime(long newTime) {
    var d = Duration.ofMillis(newTime);
    var s = String.format("%02d:%02d", d.toMinutesPart(), d.toSecondsPart());
    Platform.runLater(
        () -> {
          if (!progress.isValueChanging()) {
            timeCurrent.setText(s);
            sliderValueCurrentlyPlayerControlled = true;
            progress.setValue(newTime);
            sliderValueCurrentlyPlayerControlled = false;
          }
        });
  }

  private void updateTotalTime(long newLength) {
    var d = Duration.ofMillis(newLength);
    var s = String.format("/%02d:%02d", d.toMinutesPart(), d.toSecondsPart());
    Platform.runLater(
        () -> {
          timeTotal.setText(s);
          progress.setMax(newLength);
        });
  }
}
