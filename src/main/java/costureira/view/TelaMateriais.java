package costureira.view;

import costureira.model.Material;
import costureira.util.Formatos;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Window;
import java.util.List;

/** Caso de uso "Cadastrar materiais" (<<extend>> de Adicionar itens do serviço). */
public class TelaMateriais extends TelaCadastro<Material> {

    private static final long serialVersionUID = 1L;

    /** Abaixo dessa quantidade a linha fica destacada. */
    public static final double ESTOQUE_MINIMO = 3;

    private final JTextField tipo = Campos.texto(20);
    private final JTextField marca = Campos.texto(15);
    private final JTextField cor = Campos.texto(12);
    private final JComboBox<String> unidade = new JComboBox<>(Material.UNIDADES);
    private final JTextField qtdEstoque = Campos.texto(8);
    private final JTextField valorProduto = Campos.texto(8);
    private final JComboBox<Campos.Opcao> status = Campos.comboStatusCadastro();

    public TelaMateriais(Window dono) {
        super(dono, "Cadastro de materiais");
        unidade.setEditable(true);
        montar();
        setSize(900, 600);
        setLocationRelativeTo(dono);
    }

    @Override
    protected String[] colunas() {
        return new String[]{"Código", "Tipo", "Marca", "Cor", "Unidade", "Estoque", "Valor", "Status"};
    }

    @Override
    protected Object[] linha(Material m) {
        return new Object[]{m.getId(), m.getTipo(), m.getMarca(), m.getCor(), m.getUnidade(),
            Formatos.numero(m.getQtdEstoque()), Formatos.dinheiro(m.getValorProduto()), m.getStatusDescricao()};
    }

    @Override
    protected List<Material> listar() {
        return Material.listar();
    }

    @Override
    protected JComponent criarFormulario() {
        return new PainelForm()
                .linha("Tipo*:", tipo, "Marca:", marca)
                .linha("Cor:", cor, "Unidade*:", unidade)
                .linha("Qtd. em estoque*:", qtdEstoque, "Valor do produto*:", valorProduto)
                .linha("Status*:", status);
    }

    @Override
    protected void preencher(Material m) {
        tipo.setText(m.getTipo());
        marca.setText(m.getMarca());
        cor.setText(m.getCor());
        unidade.setSelectedItem(m.getUnidade());
        qtdEstoque.setText(Formatos.numero(m.getQtdEstoque()));
        valorProduto.setText(Formatos.valor(m.getValorProduto()));
        Campos.selecionarCodigo(status, String.valueOf(m.getStatus()));
    }

    @Override
    protected void limpar() {
        tipo.setText("");
        marca.setText("");
        cor.setText("");
        unidade.setSelectedIndex(0);
        qtdEstoque.setText("");
        valorProduto.setText("");
        status.setSelectedIndex(0);
    }

    @Override
    protected void habilitarCampos(boolean h) {
        tipo.setEnabled(h);
        marca.setEnabled(h);
        cor.setEnabled(h);
        unidade.setEnabled(h);
        qtdEstoque.setEnabled(h);
        valorProduto.setEnabled(h);
        status.setEnabled(h);
    }

    @Override
    protected Material lerFormulario() {
        Material m = new Material();
        m.setTipo(tipo.getText());
        m.setMarca(marca.getText().trim());
        m.setCor(cor.getText().trim());
        Object u = unidade.getEditor().getItem();
        m.setUnidade(u == null ? "" : u.toString().trim());
        m.setQtdEstoque(Formatos.lerDecimal(qtdEstoque.getText(), "a quantidade em estoque"));
        m.setValorProduto(Formatos.lerDecimal(valorProduto.getText(), "o valor do produto"));
        m.setStatus(Campos.codigo(status).charAt(0));
        return m;
    }

    @Override
    protected Color corLinha(Material m) {
        if (m.getStatus() == 'i') {
            return Cores.INATIVO;
        }
        return m.getQtdEstoque() < ESTOQUE_MINIMO ? Cores.ESTOQUE_BAIXO : null;
    }

    @Override
    protected String textoRodape(List<Material> registros) {
        return registros.size() + " material(is) — linhas laranja: estoque abaixo de " + Formatos.numero(ESTOQUE_MINIMO);
    }
}
