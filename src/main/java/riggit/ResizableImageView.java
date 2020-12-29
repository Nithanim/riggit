package riggit;

import javafx.geometry.Orientation;
import javafx.scene.image.ImageView;

/**
 * Currently only built for full width scaling and height is adjusted based on aspect ratio. Behaves
 * a bit like CSS "img {width: 100%}" but image is never stretched beyond original size.
 */
public class ResizableImageView extends ImageView {

  public ResizableImageView(String url) {
    super(url);
  }

  @Override
  public double minWidth(double height) {
    return 100;
  }

  @Override
  public double prefWidth(double height) {
    var image = getImage();
    if (image == null) {
      return minWidth(height);
    } else {
      return image.getWidth();
    }
  }

  @Override
  public double maxWidth(double height) {
    var image = getImage();
    if (image == null) {
      return 0;
    } else {
      return image.getWidth();
    }
  }

  @Override
  public double minHeight(double width) {
    return 100;
  }

  @Override
  public double prefHeight(double width) {
    var image = getImage();
    if (image == null) {
      return minHeight(width);
    } else {
      return image.getHeight();
    }
  }

  @Override
  public double maxHeight(double width) {
    var image = getImage();
    if (image == null) {
      return 100;
    } else {
      return image.getHeight() / image.getWidth() * width;
    }
  }

  @Override
  public Orientation getContentBias() {
    return Orientation.HORIZONTAL;
  }

  @Override
  public boolean isResizable() {
    return true;
  }

  @Override
  public void resize(double width, double height) {
    setFitWidth(width);
    setFitHeight(height);
  }
}
