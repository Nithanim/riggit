package old;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import javax.swing.*;

public class MainSwing {
  public static void main(String[] args) {
    LafManager.install(new DarculaTheme());
    // LafManager.installTheme(LafManager.getPreferredThemeStyle());
    // LafManager.enabledPreferenceChangeReporting(true); // If system settings change, we do too
    // https://github.com/weisJ/darklaf/wiki/LafManager#setting-the-theme

    SwingUtilities.invokeLater(
        () -> {
          LafManager.install();

          JFrame frame = new JFrame("Darklaf - A themeable LaF for Swing");
          frame.setSize(600, 400);

          JButton button = new JButton("Click here!");

          JPanel content = new JPanel();
          content.add(button);

          frame.setLocationRelativeTo(null);
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setContentPane(content);
          frame.setVisible(true);
        });
  }
}
