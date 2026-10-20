package costureira.model;

import costureira.dao.BancoDados;
import costureira.util.Descricoes;
import costureira.util.Formatos;

import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro de serviço (tipos de conserto que a Iraci oferece: barra, ajuste, troca de zíper...).
 * Atributos: @id, *status (a/i), *nome, descrição, *valor_padrão, *tempo_estimado (minutos).
 */
public class Servico extends Entidade {

    private static final long serialVersionUID = 1L;

    private char status = 'a';
    private String nome;
    private String descricao;
    private double valorPadrao;
    private int tempoEstimado;

    @Override
    public void salvar() {
        BancoDados bd = BancoDados.get();
        if (Formatos.vazio(nome)) {
            throw new RegraNegocioException("Informe o nome do serviço.");
        }
        for (Servico s : bd.getServicos()) {
            if (s.id != id && s.nome.equalsIgnoreCase(nome.trim())) {
                throw new RegraNegocioException("Já existe um serviço com o nome \"" + s.nome + "\".");
            }
        }
        if (valorPadrao <= 0) {
            throw new RegraNegocioException("O valor padrão deve ser maior que zero.");
        }
        if (tempoEstimado <= 0) {
            throw new RegraNegocioException("O tempo estimado (em minutos) deve ser maior que zero.");
        }
        if (status != 'a' && status != 'i') {
            throw new RegraNegocioException("Status inválido.");
        }
        nome = nome.trim();
        if (id == 0) {
            id = bd.proximoId("servico");
            bd.getServicos().add(this);
        } else {
            BancoDados.substituir(bd.getServicos(), this);
        }
        bd.gravar();
    }

    /** Regra: serviço com ordem vinculada não pode ser excluído, só inativado. */
    @Override
    public void excluir() {
        BancoDados bd = BancoDados.get();
        for (OrdemServico os : bd.getOrdens()) {
            for (ItemOrdemServico item : os.getItens()) {
                if (item.getIdServico() == id) {
                    throw new RegraNegocioException("Este serviço já foi usado na OS nº " + os.getId()
                            + " e não pode ser excluído.\nEdite o cadastro e altere o status para Inativo.");
                }
            }
        }
        bd.getServicos().removeIf(s -> s.id == id);
        bd.gravar();
    }

    public static List<Servico> listar() {
        return new ArrayList<>(BancoDados.get().getServicos());
    }

    public static List<Servico> listarAtivos() {
        List<Servico> ativos = new ArrayList<>();
        for (Servico s : BancoDados.get().getServicos()) {
            if (s.status == 'a') {
                ativos.add(s);
            }
        }
        return ativos;
    }

    public static Servico buscar(int id) {
        return BancoDados.buscar(BancoDados.get().getServicos(), id);
    }

    public String getStatusDescricao() {
        return Descricoes.statusCadastro(status);
    }

    @Override
    public String toString() {
        return nome + " - " + Formatos.dinheiro(valorPadrao);
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getValorPadrao() {
        return valorPadrao;
    }

    public void setValorPadrao(double valorPadrao) {
        this.valorPadrao = valorPadrao;
    }

    public int getTempoEstimado() {
        return tempoEstimado;
    }

    public void setTempoEstimado(int tempoEstimado) {
        this.tempoEstimado = tempoEstimado;
    }
}
