package costureira.model;

import costureira.dao.BancoDados;
import costureira.util.Descricoes;
import costureira.util.Formatos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Medidas do cliente (Cliente 1 — 0..* Medidas).
 * Atributos: @id, *status (a/i), *id_cliente, *tipo_medida, *valor (cm), *data_afericao.
 */
public class Medida extends Entidade {

    private static final long serialVersionUID = 1L;

    /** Sugestões para o campo "tipo de medida" (o campo aceita qualquer texto). */
    public static final String[] TIPOS_COMUNS = {
        "Busto", "Cintura", "Quadril", "Ombro", "Comprimento da manga", "Cava",
        "Comprimento da blusa", "Comprimento da calça", "Gancho", "Coxa", "Barra", "Pescoço"
    };

    private char status = 'a';
    private int idCliente;
    private String tipoMedida;
    private double valor;
    private LocalDate dataAfericao;

    @Override
    public void salvar() {
        BancoDados bd = BancoDados.get();
        if (Cliente.buscar(idCliente) == null) {
            throw new RegraNegocioException("Selecione o cliente.");
        }
        if (Formatos.vazio(tipoMedida)) {
            throw new RegraNegocioException("Informe o tipo de medida (ex.: Cintura).");
        }
        if (valor <= 0) {
            throw new RegraNegocioException("O valor da medida deve ser maior que zero.");
        }
        if (dataAfericao == null) {
            throw new RegraNegocioException("Informe a data em que a medida foi tirada.");
        }
        if (dataAfericao.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data da medida não pode ser no futuro.");
        }
        tipoMedida = tipoMedida.trim();
        if (id == 0) {
            id = bd.proximoId("medida");
            bd.getMedidas().add(this);
        } else {
            BancoDados.substituir(bd.getMedidas(), this);
        }
        bd.gravar();
    }

    @Override
    public void excluir() {
        BancoDados bd = BancoDados.get();
        bd.getMedidas().removeIf(m -> m.id == id);
        bd.gravar();
    }

    public static List<Medida> listarDoCliente(int idCliente) {
        List<Medida> lista = new ArrayList<>();
        for (Medida m : BancoDados.get().getMedidas()) {
            if (idCliente == 0 || m.idCliente == idCliente) {
                lista.add(m);
            }
        }
        return lista;
    }

    public String getStatusDescricao() {
        return Descricoes.statusCadastro(status);
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getTipoMedida() {
        return tipoMedida;
    }

    public void setTipoMedida(String tipoMedida) {
        this.tipoMedida = tipoMedida;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDate getDataAfericao() {
        return dataAfericao;
    }

    public void setDataAfericao(LocalDate dataAfericao) {
        this.dataAfericao = dataAfericao;
    }
}
