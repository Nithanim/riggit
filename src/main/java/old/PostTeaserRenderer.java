package old;

import java.awt.*;
import javax.swing.*;
import old.PostTeaser;
import old.PostTeaserPanel;

public class PostTeaserRenderer implements ListCellRenderer<PostTeaser> {

  @Override
  public Component getListCellRendererComponent(
      JList<? extends PostTeaser> list,
      PostTeaser value,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {

    // JLabel lbl = new JLabel(value.getTitle() + "\n" + value.getText());
    Component c = new PostTeaserPanel(value).getPanel1();

    if (isSelected) {
      c.setBackground(list.getSelectionBackground());
      c.setForeground(list.getSelectionForeground());
    } else {
      c.setBackground(list.getBackground());
      c.setForeground(list.getForeground());
    }

    return c;
  }
}
