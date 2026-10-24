package atm;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ATMGUI gui = new ATMGUI();
            gui.setVisible(true);
        });
    }
}
