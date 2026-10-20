package costureira.model;

import costureira.dao.BancoDados;
import costureira.util.Descricoes;
import costureira.util.Formatos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Movimentação: recebimento — sempre à vista, na entrega das roupas consertadas.
 * Atributos: @id, *status (a = recebido / c = cancelado), *id_ordem, *data_recebimento,
 * *forma_pagto / método de recebimento (d/p/db/cr), *valor_recebido, retirada_por, obs.
 *
 * Registrar o recebimento finaliza a ordem (entrega). Cada ordem tem no máximo um recebimento ativo.
 */
public class Recebimento extends Entidade {

    private static final long serialVersionUID = 1L;

    private char status = 'a';
    private int idOrdem;
    private LocalDate dataRecebimento;
    private String formaPagto;
    private double valorRecebido;
    private String retiradaPor;
    private String obs;

    @Override
    public void salvar() {
        BancoDados bd = BancoDados.get();
        Recebimento antigo = id == 0 ? null : buscar(id);
        if (antigo != null && antigo.status == 'c') {
            throw new RegraNegocioException("Recebimento cancelado não pode ser editado.");
        }
        if (antigo != null) {
            idOrdem = antigo.idOrdem; // a ordem do recebimento não muda
        }
        OrdemServico os = OrdemServico.buscar(idOrdem);
        if (os == null) {
            throw new RegraNegocioException("Selecione a ordem de serviço.");
        }
        if (antigo == null) {
            if (os.getStatus() == 'c') {
                throw new RegraNegocioException("Não é possível registrar recebimento de uma ordem cancelada.");
            }
            if (os.getStatus() == 'f' || os.getRecebimento() != null) {
                throw new RegraNegocioException("Esta ordem já foi entregue e possui recebimento.");
            }
            if (os.getStatus() == 'a') {
                throw new RegraNegocioException("A ordem ainda está agendada. Inicie a execução antes de registrar a entrega.");
            }
        }
        if (dataRecebimento == null) {
            throw new RegraNegocioException("Informe a data do recebimento.");
        }
        if (dataRecebimento.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data do recebimento não pode ser no futuro.");
        }
        if (dataRecebimento.isBefore(os.getDataAgendamento())) {
            throw new RegraNegocioException("A data do recebimento não pode ser anterior ao agendamento ("
                    + Formatos.data(os.getDataAgendamento()) + ").");
        }
        if (formaPagto == null || !Arrays.asList(Descricoes.FORMAS_PAGTO).contains(formaPagto)) {
            throw new RegraNegocioException("Selecione a forma de pagamento.");
        }
        double total = os.calcularValorTotal();
        if (Math.abs(valorRecebido - total) > 0.005) {
            throw new RegraNegocioException("O valor recebido deve ser igual ao valor total da ordem ("
                    + Formatos.dinheiro(total) + ").\nO pagamento é sempre à vista, na entrega: sem parcelamento nem adiantamento.");
        }
        if (Formatos.vazio(retiradaPor)) {
            retiradaPor = os.getNomeCliente();
        }
        retiradaPor = retiradaPor.trim();
        status = 'a';

        if (antigo == null) {
            id = bd.proximoId("recebimento");
            bd.getRecebimentos().add(this);
        } else {
            BancoDados.substituir(bd.getRecebimentos(), this);
        }
        // entrega: a ordem é finalizada
        os.setStatus('f');
        os.setDataEntrega(dataRecebimento);
        os.setRetiradaPor(retiradaPor);
        bd.gravar();
    }

    /** Estorno: cancela o recebimento e a ordem volta para "em execução". */
    public void cancelar() {
        if (status == 'c') {
            throw new RegraNegocioException("Este recebimento já está cancelado.");
        }
        status = 'c';
        OrdemServico os = OrdemServico.buscar(idOrdem);
        if (os != null) {
            os.setStatus('e');
            os.setDataEntrega(null);
            os.setRetiradaPor(null);
        }
        BancoDados.get().gravar();
    }

    /** Recebimentos não são apagados (histórico financeiro): use cancelar(). */
    @Override
    public void excluir() {
        throw new RegraNegocioException("Recebimentos não podem ser excluídos. Use Cancelar recebimento.");
    }

    public static List<Recebimento> listar() {
        return new ArrayList<>(BancoDados.get().getRecebimentos());
    }

    public static Recebimento buscar(int id) {
        return BancoDados.buscar(BancoDados.get().getRecebimentos(), id);
    }

    public OrdemServico getOrdem() {
        return OrdemServico.buscar(idOrdem);
    }

    public String getStatusDescricao() {
        return Descricoes.statusRecebimento(status);
    }

    public String getFormaPagtoDescricao() {
        return Descricoes.formaPagto(formaPagto);
    }

    public char getStatus() {
        return status;
    }

    public int getIdOrdem() {
        return idOrdem;
    }

    public void setIdOrdem(int idOrdem) {
        this.idOrdem = idOrdem;
    }

    public LocalDate getDataRecebimento() {
        return dataRecebimento;
    }

    public void setDataRecebimento(LocalDate dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }

    public String getFormaPagto() {
        return formaPagto;
    }

    public void setFormaPagto(String formaPagto) {
        this.formaPagto = formaPagto;
    }

    public double getValorRecebido() {
        return valorRecebido;
    }

    public void setValorRecebido(double valorRecebido) {
        this.valorRecebido = valorRecebido;
    }

    public String getRetiradaPor() {
        return retiradaPor;
    }

    public void setRetiradaPor(String retiradaPor) {
        this.retiradaPor = retiradaPor;
    }

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }
}
