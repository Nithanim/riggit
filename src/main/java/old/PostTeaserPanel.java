package old;

import java.awt.*;
import javax.swing.*;
import lombok.Getter;

public class PostTeaserPanel {
    private JTextPane textPane1;
    @Getter
    private JPanel panel1;

    public PostTeaserPanel(PostTeaser postTeaser) {
        textPane1.setText("<html><body><strong>" + postTeaser.getTitle() + "</strong><br/>r/" + postTeaser.getSubreddit() + "</body></html>");
    }

    {
        // GUI initializer generated by IntelliJ IDEA GUI Designer
        // >>> IMPORTANT!! <<<
        // DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 10));
        panel1.setEnabled(true);
        textPane1 = new JTextPane();
        textPane1.setContentType("text/html");
        panel1.add(textPane1, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
