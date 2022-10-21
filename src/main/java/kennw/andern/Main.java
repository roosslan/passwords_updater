package kennw.andern;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // остаётся стандартный вид
                }
                // systems.xml и kennw.log ищутся в текущей директории
                new MainFrame(new File("systems.xml"), new FileLog(new File("kennw.log"))).setVisible(true);
            }
        });
    }
}
