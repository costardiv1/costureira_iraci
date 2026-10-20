package costureira.view;

import costureira.model.Cliente;
import costureira.util.Formatos;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Window;
import java.util.Collections;
import java.util.List;

/** Caso de uso "Cadastrar cliente" (estendido por "Cadastrar medidas"). */
public class TelaClientes extends TelaCadastro<Cliente> {

    private static final long serialVersionUID = 1L;

    private final JTextField nome = Campos.texto(30);
    private final JTextField cpf = Campos.texto(14);
    private final JTextField telefone = Campos.texto(15);
    private final JTextField email = Campos.texto(25);
    private final JFormattedTextField dtNasc = Campos.data();
    private final JTextField endereco = Campos.texto(40);
    private final JComboBox<Campos.Opcao> genero = Campos.comboOpcoes(
            new Campos.Opcao("n", "Não informado"), new Campos.Opcao("f", "Feminino"), new Campos.Opcao("m", "Masculino"));
    private final JComboBox<Campos.Opcao> status = Campos.comboStatusCadastro();
    private final JTextField dtCadastro = Campos.texto(10);
    private final JButton btMedidas = new JButton("Medidas do cliente...");

    public TelaClientes(Window dono) {
        super(dono, "Cadastro de clientes");
        dtCadastro.setEditable(false);
        btMedidas.addActionListener(e -> abrirMedidas());
        montar();
    }

    @Override
    protected String[] colunas() {
        return new String[]{"Código", "Nome", "CPF", "Telefone", "E-mail", "Status"};
    }

    @Override
    protected int[] largurasColunas() {
        return new int[]{50, 250, 110, 110, 200, 60};
    }

    @Override
    protected Object[] linha(Cliente c) {
        return new Object[]{c.getId(), c.getNome(), c.getCpf(), c.getTelefone(), c.getEmail(), c.getStatusDescricao()};
    }

    @Override
    protected List<Cliente> listar() {
        return Cliente.listar();
    }

    @Override
    protected JComponent criarFormulario() {
        return new PainelForm()
                .linha("Nome*:", nome)
                .linha("CPF*:", cpf, "Telefone*:", telefone)
                .linha("E-mail:", email, "Data de nascimento:", dtNasc)
                .linha("Endereço:", endereco)
                .linha("Gênero:", genero, "Status*:", status)
                .linha("Data de cadastro:", dtCadastro);
    }

    @Override
    protected void preencher(Cliente c) {
        nome.setText(c.getNome());
        cpf.setText(c.getCpf());
        telefone.setText(c.getTelefone());
        email.setText(c.getEmail());
        Campos.setData(dtNasc, c.getDtNasc());
        endereco.setText(c.getEndereco());
        Campos.selecionarCodigo(genero, String.valueOf(c.getGenero()));
        Campos.selecionarCodigo(status, String.valueOf(c.getStatus()));
        dtCadastro.setText(Formatos.data(c.getDtCadastro()));
    }

    @Override
    protected void limpar() {
        nome.setText("");
        cpf.setText("");
        telefone.setText("");
        email.setText("");
        Campos.setData(dtNasc, null);
        endereco.setText("");
        genero.setSelectedIndex(0);
        status.setSelectedIndex(0);
        dtCadastro.setText("");
    }

    @Override
    protected void habilitarCampos(boolean h) {
        nome.setEnabled(h);
        cpf.setEnabled(h);
        telefone.setEnabled(h);
        email.setEnabled(h);
        dtNasc.setEnabled(h);
        endereco.setEnabled(h);
        genero.setEnabled(h);
        status.setEnabled(h);
    }

    @Override
    protected Cliente lerFormulario() {
        Cliente c = new Cliente();
        c.setNome(nome.getText());
        c.setCpf(cpf.getText());
        c.setTelefone(telefone.getText());
        c.setEmail(email.getText().trim());
        c.setDtNasc(Formatos.lerData(dtNasc.getText(), "a data de nascimento", false));
        c.setEndereco(endereco.getText().trim());
        c.setGenero(Campos.codigo(genero).charAt(0));
        c.setStatus(Campos.codigo(status).charAt(0));
        return c;
    }

    @Override
    protected List<JButton> botoesExtras() {
        return Collections.singletonList(btMedidas);
    }

    @Override
    protected void atualizarBotoesExtras(Cliente selecionado, boolean editando) {
        btMedidas.setEnabled(selecionado != null);
    }

    @Override
    protected Color corLinha(Cliente c) {
        return c.getStatus() == 'i' ? Cores.INATIVO : null;
    }

    /** <<extend>> Cadastrar medidas. */
    private void abrirMedidas() {
        if (getSelecionado() != null) {
            new TelaMedidas(this, getSelecionado()).setVisible(true);
        }
    }
}
