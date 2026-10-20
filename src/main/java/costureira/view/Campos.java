package costureira.view;

import costureira.model.Entidade;
import costureira.util.Formatos;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Fábrica de campos de formulário (data, hora) e utilidades para combos.
 */
public final class Campos {

    private Campos() {
    }

    public static JFormattedTextField data() {
        return mascara("##/##/####", 10);
    }

    public static JFormattedTextField hora() {
        return mascara("##:##", 5);
    }

    private static JFormattedTextField mascara(String mascara, int colunas) {
        try {
            MaskFormatter mf = new MaskFormatter(mascara);
            mf.setPlaceholderCharacter('_');
            JFormattedTextField campo = new JFormattedTextField(mf);
            campo.setFocusLostBehavior(JFormattedTextField.PERSIST);
            campo.setColumns(colunas);
            return campo;
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void setData(JFormattedTextField campo, LocalDate data) {
        campo.setValue(data == null ? null : Formatos.data(data));
    }

    public static void setHora(JFormattedTextField campo, LocalTime hora) {
        campo.setValue(hora == null ? null : Formatos.hora(hora));
    }

    public static JTextField texto(int colunas) {
        return new JTextField(colunas);
    }

    /** Preenche o combo; se "atual" não estiver na lista (ex.: inativo), ele é incluído. */
    public static <T extends Entidade> void preencher(JComboBox<T> combo, List<T> itens, T atual) {
        combo.removeAllItems();
        boolean achou = atual == null;
        for (T t : itens) {
            combo.addItem(t);
            if (atual != null && t.getId() == atual.getId()) {
                achou = true;
            }
        }
        if (!achou) {
            combo.addItem(atual);
        }
        selecionar(combo, atual == null ? 0 : atual.getId());
    }

    public static <T extends Entidade> void selecionar(JComboBox<T> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1);
    }

    /** Opção de combo com código e descrição, ex.: 'a' = "Ativo". */
    public static class Opcao {
        public final String codigo;
        public final String descricao;

        public Opcao(String codigo, String descricao) {
            this.codigo = codigo;
            this.descricao = descricao;
        }

        @Override
        public String toString() {
            return descricao;
        }
    }

    public static JComboBox<Opcao> comboOpcoes(Opcao... opcoes) {
        return new JComboBox<>(opcoes);
    }

    public static String codigo(JComboBox<Opcao> combo) {
        Opcao o = (Opcao) combo.getSelectedItem();
        return o == null ? null : o.codigo;
    }

    public static void selecionarCodigo(JComboBox<Opcao> combo, String codigo) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).codigo.equals(codigo)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1);
    }

    public static JComboBox<Opcao> comboStatusCadastro() {
        return comboOpcoes(new Opcao("a", "Ativo"), new Opcao("i", "Inativo"));
    }
}
