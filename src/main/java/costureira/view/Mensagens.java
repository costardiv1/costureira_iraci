package costureira.view;

import javax.swing.JOptionPane;
import java.awt.Component;

public final class Mensagens {

    private Mensagens() {
    }

    public static void aviso(Component pai, String texto) {
        JOptionPane.showMessageDialog(pai, texto, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component pai, String texto) {
        JOptionPane.showMessageDialog(pai, texto, "Costureira Iraci", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void erro(Component pai, String texto) {
        JOptionPane.showMessageDialog(pai, texto, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component pai, String texto) {
        return JOptionPane.showConfirmDialog(pai, texto, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
