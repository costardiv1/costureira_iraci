package costureira.view;

import costureira.model.Servico;
import costureira.util.Formatos;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Window;
import java.util.List;

/** Cadastro dos serviços oferecidos (barra, ajuste, troca de zíper...). */
public class TelaServicos extends TelaCadastro<Servico> {

    private static final long serialVersionUID = 1L;

    private final JTextField nome = Campos.texto(30);
    private final JTextArea descricao = new JTextArea(3, 30);
    private final JTextField valorPadrao = Campos.texto(8);
    private final JTextField tempoEstimado = Campos.texto(6);
    private final JComboBox<Campos.Opcao> status = Campos.comboStatusCadastro();

    public TelaServicos(Window dono) {
        super(dono, "Cadastro de serviços");
        descricao.setLineWrap(true);
        descricao.setWrapStyleWord(true);
        montar();
        setSize(900, 600);
        setLocationRelativeTo(dono);
    }

    @Override
    protected String[] colunas() {
        return new String[]{"Código", "Nome", "Valor padrão", "Tempo (min)", "Status"};
    }

    @Override
    protected int[] largurasColunas() {
        return new int[]{50, 300, 100, 90, 60};
    }

    @Override
    protected Object[] linha(Servico s) {
        return new Object[]{s.getId(), s.getNome(), Formatos.dinheiro(s.getValorPadrao()), s.getTempoEstimado(), s.getStatusDescricao()};
    }

    @Override
    protected List<Servico> listar() {
        return Servico.listar();
    }

    @Override
    protected JComponent criarFormulario() {
        return new PainelForm()
                .linha("Nome*:", nome)
                .linha("Descrição:", new JScrollPane(descricao))
                .linha("Valor padrão (R$)*:", valorPadrao, "Tempo estimado (min)*:", tempoEstimado)
                .linha("Status*:", status);
    }

    @Override
    protected void preencher(Servico s) {
        nome.setText(s.getNome());
        descricao.setText(s.getDescricao());
        valorPadrao.setText(Formatos.valor(s.getValorPadrao()));
        tempoEstimado.setText(String.valueOf(s.getTempoEstimado()));
        Campos.selecionarCodigo(status, String.valueOf(s.getStatus()));
    }

    @Override
    protected void limpar() {
        nome.setText("");
        descricao.setText("");
        valorPadrao.setText("");
        tempoEstimado.setText("");
        status.setSelectedIndex(0);
    }

    @Override
    protected void habilitarCampos(boolean h) {
        nome.setEnabled(h);
        descricao.setEnabled(h);
        valorPadrao.setEnabled(h);
        tempoEstimado.setEnabled(h);
        status.setEnabled(h);
    }

    @Override
    protected Servico lerFormulario() {
        Servico s = new Servico();
        s.setNome(nome.getText());
        s.setDescricao(descricao.getText().trim());
        s.setValorPadrao(Formatos.lerDecimal(valorPadrao.getText(), "o valor padrão"));
        s.setTempoEstimado(Formatos.lerInteiro(tempoEstimado.getText(), "o tempo estimado"));
        s.setStatus(Campos.codigo(status).charAt(0));
        return s;
    }

    @Override
    protected Color corLinha(Servico s) {
        return s.getStatus() == 'i' ? Cores.INATIVO : null;
    }
}
