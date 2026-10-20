package costureira.view;

import costureira.model.Cliente;
import costureira.model.Medida;
import costureira.util.Formatos;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso "Cadastrar medidas" (<<extend>> de Cadastrar cliente).
 * Aberta pelo cadastro de clientes (só as medidas daquele cliente) ou pelo menu (todos).
 */
public class TelaMedidas extends TelaCadastro<Medida> {

    private static final long serialVersionUID = 1L;

    /** Cliente fixo (aberta a partir do cadastro de clientes) ou null. */
    private final Cliente clienteFixo;

    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private final JComboBox<String> tipoMedida = new JComboBox<>(Medida.TIPOS_COMUNS);
    private final JTextField valor = Campos.texto(8);
    private final JFormattedTextField dataAfericao = Campos.data();
    private final JComboBox<Campos.Opcao> status = Campos.comboStatusCadastro();

    public TelaMedidas(Window dono, Cliente clienteFixo) {
        super(dono, clienteFixo == null ? "Medidas dos clientes" : "Medidas de " + clienteFixo.getNome());
        this.clienteFixo = clienteFixo;
        tipoMedida.setEditable(true);
        montar();
        setSize(800, 560);
        setLocationRelativeTo(dono);
    }

    @Override
    protected String[] colunas() {
        return new String[]{"Código", "Cliente", "Medida", "Valor (cm)", "Data", "Status"};
    }

    @Override
    protected int[] largurasColunas() {
        return new int[]{50, 200, 160, 80, 90, 60};
    }

    @Override
    protected Object[] linha(Medida m) {
        Cliente c = Cliente.buscar(m.getIdCliente());
        return new Object[]{m.getId(), c == null ? "" : c.getNome(), m.getTipoMedida(),
            Formatos.numero(m.getValor()), Formatos.data(m.getDataAfericao()), m.getStatusDescricao()};
    }

    @Override
    protected List<Medida> listar() {
        return Medida.listarDoCliente(clienteFixo == null ? 0 : clienteFixo.getId());
    }

    @Override
    protected JComponent criarFormulario() {
        return new PainelForm()
                .linha("Cliente*:", cliente)
                .linha("Tipo de medida*:", tipoMedida, "Valor (cm)*:", valor)
                .linha("Data da medição*:", dataAfericao, "Status*:", status);
    }

    @Override
    protected void atualizarListas(Medida atual) {
        if (clienteFixo != null) {
            Cliente c = Cliente.buscar(clienteFixo.getId());
            Campos.preencher(cliente, java.util.Collections.singletonList(c), c);
            return;
        }
        Cliente doRegistro = atual == null ? null : Cliente.buscar(atual.getIdCliente());
        Campos.preencher(cliente, Cliente.listar(), doRegistro);
    }

    @Override
    protected void preencher(Medida m) {
        Campos.selecionar(cliente, m.getIdCliente());
        tipoMedida.setSelectedItem(m.getTipoMedida());
        valor.setText(Formatos.numero(m.getValor()));
        Campos.setData(dataAfericao, m.getDataAfericao());
        Campos.selecionarCodigo(status, String.valueOf(m.getStatus()));
    }

    @Override
    protected void limpar() {
        if (clienteFixo == null) {
            cliente.setSelectedIndex(-1);
        }
        tipoMedida.setSelectedItem("");
        valor.setText("");
        Campos.setData(dataAfericao, null);
        status.setSelectedIndex(0);
    }

    @Override
    protected void valoresIniciais() {
        if (clienteFixo != null) {
            Campos.selecionar(cliente, clienteFixo.getId());
        }
        Campos.setData(dataAfericao, LocalDate.now());
    }

    @Override
    protected void habilitarCampos(boolean h) {
        cliente.setEnabled(h && clienteFixo == null);
        tipoMedida.setEnabled(h);
        valor.setEnabled(h);
        dataAfericao.setEnabled(h);
        status.setEnabled(h);
    }

    @Override
    protected Medida lerFormulario() {
        Medida m = new Medida();
        Cliente c = (Cliente) cliente.getSelectedItem();
        m.setIdCliente(c == null ? 0 : c.getId());
        Object tipo = tipoMedida.getEditor().getItem();
        m.setTipoMedida(tipo == null ? "" : tipo.toString());
        m.setValor(Formatos.lerDecimal(valor.getText(), "o valor da medida"));
        m.setDataAfericao(Formatos.lerData(dataAfericao.getText(), "a data da medição", true));
        m.setStatus(Campos.codigo(status).charAt(0));
        return m;
    }
}
