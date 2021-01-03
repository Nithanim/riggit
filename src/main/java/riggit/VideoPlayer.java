package riggit;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Getter;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class VideoPlayer {
  private final String videoUrl;
  @Getter BorderPane root;
  @Getter StackPane stack;
  ImageView imageView;

  private MediaPlayerFactory mediaPlayerFactory;
  private EmbeddedMediaPlayer mediaPlayer;

  public VideoPlayer(String thumbnailUrl, String videoUrl) {
    this.videoUrl = videoUrl;
    this.root = new BorderPane();
    this.stack = new StackPane();
    root.setCenter(stack);
    stack.maxHeight(Pane.USE_COMPUTED_SIZE);
    stack.maxWidth(Pane.USE_COMPUTED_SIZE);
    if (thumbnailUrl != null) {
      this.imageView = new ImageView(thumbnailUrl);
    } else {
      var wi = new WritableImage(200, 200);
      var pixelWriter = wi.getPixelWriter();
      Color color = Color.color(0.6, 0, 0);
      for (int y = 0; y < 200; y++) {
        for (int x = 0; x < 200; x++) {
          pixelWriter.setColor(x, y, color);
        }
      }

      this.imageView = new ImageView(wi);
    }
    stack.getChildren().add(imageView);
    var icon = new Label("Video");
    stack.getChildren().add(icon);
    StackPane.setAlignment(icon, Pos.BOTTOM_LEFT);
    imageView.setOnMouseClicked(this::transformToVideoPlayback);
  }

  @Override
  protected void finalize() throws Throwable {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer.controls().stop();
    }
    if (mediaPlayerFactory != null) {
      mediaPlayerFactory.release();
    }
  }

  private void transformToVideoPlayback(MouseEvent e) {
    stack.getChildren().remove(1);
    imageView.setOnMouseClicked(null);
    imageView.setPreserveRatio(true);

    mediaPlayerFactory = new MediaPlayerFactory();
    mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
    mediaPlayer
        .videoSurface()
        .set(ImageViewVideoSurfaceFactory.videoSurfaceForImageView(imageView));
    mediaPlayer.media().play(this.videoUrl);
    mediaPlayer.audio().setVolume(10);

    this.mediaPlayer
        .events()
        .addMediaPlayerEventListener(
            new MediaPlayerEventAdapter() {
              @Override
              public void playing(MediaPlayer mediaPlayer) {}

              @Override
              public void paused(MediaPlayer mediaPlayer) {}

              @Override
              public void stopped(MediaPlayer mediaPlayer) {}

              @Override
              public void timeChanged(MediaPlayer mediaPlayer, long newTime) {}
            });
  }
}
