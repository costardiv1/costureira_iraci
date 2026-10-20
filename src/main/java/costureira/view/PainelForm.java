package costureira.view;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Painel de formulário em grade: "Rótulo: [campo]   Rótulo: [campo]".
 */
public class PainelForm extends JPanel {

    private static final long serialVersionUID = 1L;

    private int linha;

    public PainelForm() {
        super(new GridBagLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    /** Um campo ocupando a linha inteira. */
    public PainelForm linha(String rotulo, JComponent campo) {
        adicionar(rotulo, campo, 0, 3);
        linha++;
        return this;
    }

    /** Dois campos lado a lado. */
    public PainelForm linha(String rotulo1, JComponent campo1, String rotulo2, JComponent campo2) {
        adicionar(rotulo1, campo1, 0, 1);
        adicionar(rotulo2, campo2, 2, 1);
        linha++;
        return this;
    }

    private void adicionar(String rotulo, JComponent campo, int coluna, int largura) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = coluna;
        g.gridy = linha;
        g.anchor = GridBagConstraints.LINE_END;
        g.insets = new Insets(3, coluna == 0 ? 0 : 16, 3, 6);
        add(new JLabel(rotulo), g);

        g = new GridBagConstraints();
        g.gridx = coluna + 1;
        g.gridy = linha;
        g.gridwidth = largura;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.LINE_START;
        g.insets = new Insets(3, 0, 3, 0);
        add(campo, g);
    }
}
