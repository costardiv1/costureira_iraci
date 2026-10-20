package costureira.model;

import costureira.dao.BancoDados;
import costureira.util.Descricoes;
import costureira.util.Formatos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Movimentação: ordem de serviço / agendamento (o "Servico" do diagrama de classes).
 * Atributos: @id, *status (a/c/e/f), *id_cliente, *data_agendamento, *hora_agendamento,
 * *data_prevista_entrega, data_entrega (data de retirada), retirada_por, **valor_total, obs.
 *
 * Ciclo: nasce Agendada (a) → Em execução (e) → Finalizada (f, ao registrar o recebimento).
 * Pode ser Cancelada (c) enquanto não estiver finalizada.
 */
public class OrdemServico extends Entidade {

    private static final long serialVersionUID = 1L;

    private char status = 'a';
    private int idCliente;
    private LocalDate dataAgendamento;
    private LocalTime horaAgendamento;
    private LocalDate dataPrevistaEntrega;
    private LocalDate dataEntrega;
    private String retiradaPor;
    private String obs;
    private double valorTotal;
    private List<ItemOrdemServico> itens = new ArrayList<>();
    private List<ItemMaterial> materiais = new ArrayList<>();

    @Override
    public void salvar() {
        BancoDados bd = BancoDados.get();
        OrdemServico antiga = id == 0 ? null : buscar(id);
        if (antiga != null && (antiga.status == 'f' || antiga.status == 'c')) {
            throw new RegraNegocioException("Ordem finalizada ou cancelada não pode mais ser editada.");
        }

        Cliente cliente = Cliente.buscar(idCliente);
        if (cliente == null) {
            throw new RegraNegocioException("Selecione o cliente.");
        }
        boolean clienteNovo = antiga == null || antiga.idCliente != idCliente;
        if (clienteNovo && cliente.getStatus() != 'a') {
            throw new RegraNegocioException("O cliente " + cliente.getNome() + " está inativo. Só é possível registrar ordem para cliente ativo.");
        }
        if (dataAgendamento == null) {
            throw new RegraNegocioException("Informe a data do agendamento.");
        }
        if (horaAgendamento == null) {
            throw new RegraNegocioException("Informe a hora do agendamento.");
        }
        if (dataPrevistaEntrega == null) {
            throw new RegraNegocioException("Informe a data prevista de entrega.");
        }
        if (dataPrevistaEntrega.isBefore(dataAgendamento)) {
            throw new RegraNegocioException("A data prevista de entrega não pode ser anterior à data de agendamento.");
        }

        if (itens.isEmpty()) {
            throw new RegraNegocioException("A ordem deve ter pelo menos um item (peça e serviço).");
        }
        for (ItemOrdemServico item : itens) {
            Servico s = Servico.buscar(item.getIdServico());
            if (s == null) {
                throw new RegraNegocioException("Serviço do item \"" + item.getDescricaoPeca() + "\" não encontrado.");
            }
            if (item.getId() == 0 && s.getStatus() != 'a') {
                throw new RegraNegocioException("O serviço " + s.getNome() + " está inativo e não pode ser incluído.");
            }
            if (Formatos.vazio(item.getDescricaoPeca())) {
                throw new RegraNegocioException("Informe a descrição da peça de todos os itens.");
            }
            if (item.getQuantidade() <= 0) {
                throw new RegraNegocioException("A quantidade de cada item deve ser maior que zero.");
            }
        }
        for (ItemMaterial im : materiais) {
            Material m = Material.buscar(im.getIdMaterial());
            if (m == null) {
                throw new RegraNegocioException("Material \"" + im.getDescricaoMaterial() + "\" não encontrado.");
            }
            if (im.getId() == 0 && m.getStatus() != 'a') {
                throw new RegraNegocioException("O material " + m.getDescricao() + " está inativo e não pode ser incluído.");
            }
            if (im.getQuantidade() <= 0) {
                throw new RegraNegocioException("A quantidade de cada material deve ser maior que zero.");
            }
        }

        // estoque: compara o que a ordem usava antes com o que usa agora
        Map<Integer, Double> diferenca = consumoPorMaterial(this);
        if (antiga != null) {
            for (Map.Entry<Integer, Double> e : consumoPorMaterial(antiga).entrySet()) {
                diferenca.merge(e.getKey(), -e.getValue(), Double::sum);
            }
        }
        for (Map.Entry<Integer, Double> e : diferenca.entrySet()) {
            Material m = Material.buscar(e.getKey());
            if (e.getValue() > 1e-9 && m.getQtdEstoque() + 1e-9 < e.getValue()) {
                throw new RegraNegocioException("Estoque insuficiente de " + m.getDescricao() + ".\n"
                        + "Disponível: " + Formatos.numero(m.getQtdEstoque()) + " " + m.getUnidade()
                        + " | Necessário: " + Formatos.numero(e.getValue()) + " " + m.getUnidade());
            }
        }

        // tudo validado: grava
        for (Map.Entry<Integer, Double> e : diferenca.entrySet()) {
            Material m = Material.buscar(e.getKey());
            m.setQtdEstoque(m.getQtdEstoque() - e.getValue());
        }
        if (antiga == null) {
            id = bd.proximoId("ordem");
            status = 'a';
            dataEntrega = null;
            retiradaPor = null;
        } else {
            status = antiga.status;
            dataEntrega = antiga.dataEntrega;
            retiradaPor = antiga.retiradaPor;
        }
        for (ItemOrdemServico item : itens) {
            if (item.getId() == 0) {
                item.setId(bd.proximoId("itemOrdem"));
            }
            item.setIdOrdem(id);
        }
        for (ItemMaterial im : materiais) {
            if (im.getId() == 0) {
                im.setId(bd.proximoId("itemMaterial"));
            }
            im.setIdOrdem(id);
        }
        valorTotal = calcularValorTotal();
        if (antiga == null) {
            bd.getOrdens().add(this);
        } else {
            BancoDados.substituir(bd.getOrdens(), this);
        }
        bd.gravar();
    }

    private static Map<Integer, Double> consumoPorMaterial(OrdemServico os) {
        Map<Integer, Double> consumo = new HashMap<>();
        for (ItemMaterial im : os.materiais) {
            consumo.merge(im.getIdMaterial(), im.getQuantidade(), Double::sum);
        }
        return consumo;
    }

    /** Agendada → Em execução. */
    public void iniciarExecucao() {
        if (status != 'a') {
            throw new RegraNegocioException("Só é possível iniciar a execução de uma ordem agendada.");
        }
        status = 'e';
        BancoDados.get().gravar();
    }

    /** Cancela a ordem e devolve os materiais ao estoque. */
    public void cancelar() {
        if (status == 'f') {
            throw new RegraNegocioException("Ordem finalizada não pode ser cancelada.");
        }
        if (status == 'c') {
            throw new RegraNegocioException("Esta ordem já está cancelada.");
        }
        devolverMateriais();
        status = 'c';
        BancoDados.get().gravar();
    }

    /** Só ordens agendadas ou canceladas podem ser excluídas. */
    @Override
    public void excluir() {
        if (status == 'e' || status == 'f') {
            throw new RegraNegocioException("Só é possível excluir ordens agendadas ou canceladas.\n"
                    + "Ordem em execução pode ser cancelada; ordem finalizada faz parte do histórico.");
        }
        if (status == 'a') {
            devolverMateriais();
        }
        BancoDados bd = BancoDados.get();
        bd.getOrdens().removeIf(o -> o.id == id);
        bd.getRecebimentos().removeIf(r -> r.getIdOrdem() == id);
        bd.gravar();
    }

    private void devolverMateriais() {
        for (ItemMaterial im : materiais) {
            Material m = Material.buscar(im.getIdMaterial());
            if (m != null) {
                m.setQtdEstoque(m.getQtdEstoque() + im.getQuantidade());
            }
        }
    }

    /** Valor total = soma dos subtotais dos itens + materiais. */
    public double calcularValorTotal() {
        return getTotalServicos() + getTotalMateriais();
    }

    public double getTotalServicos() {
        double total = 0;
        for (ItemOrdemServico item : itens) {
            total += item.getSubtotal();
        }
        return total;
    }

    public double getTotalMateriais() {
        double total = 0;
        for (ItemMaterial im : materiais) {
            total += im.getSubtotal();
        }
        return total;
    }

    /** Ordem ainda não entregue com data prevista de entrega vencida. */
    public boolean isAtrasada() {
        return isPendente() && dataPrevistaEntrega != null && dataPrevistaEntrega.isBefore(LocalDate.now());
    }

    /** Agendada ou em execução (ainda não entregue). */
    public boolean isPendente() {
        return status == 'a' || status == 'e';
    }

    public Cliente getCliente() {
        return Cliente.buscar(idCliente);
    }

    public String getNomeCliente() {
        Cliente c = getCliente();
        return c == null ? "(cliente removido)" : c.getNome();
    }

    public String getStatusDescricao() {
        return Descricoes.statusOrdem(status);
    }

    /** Recebimento ativo desta ordem, ou null. */
    public Recebimento getRecebimento() {
        for (Recebimento r : BancoDados.get().getRecebimentos()) {
            if (r.getIdOrdem() == id && r.getStatus() == 'a') {
                return r;
            }
        }
        return null;
    }

    /** Texto com o resumo da ordem (usado em "Consultar serviço"). */
    public String getResumo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS nº ").append(id).append(" - ").append(getStatusDescricao()).append('\n');
        sb.append("Cliente: ").append(getNomeCliente()).append('\n');
        sb.append("Agendada para ").append(Formatos.data(dataAgendamento)).append(" às ").append(Formatos.hora(horaAgendamento))
          .append(" | Entrega prevista: ").append(Formatos.data(dataPrevistaEntrega)).append('\n');
        if (dataEntrega != null) {
            sb.append("Entregue em ").append(Formatos.data(dataEntrega));
            if (!Formatos.vazio(retiradaPor)) {
                sb.append(", retirada por ").append(retiradaPor);
            }
            sb.append('\n');
        }
        sb.append("\nPeças / serviços:\n");
        for (ItemOrdemServico item : itens) {
            sb.append("  ").append(item.getQuantidade()).append("x ").append(item.getDescricaoPeca())
              .append(" - ").append(item.getNomeServico())
              .append(" (").append(Formatos.dinheiro(item.getValorUnitario())).append(" cada) = ")
              .append(Formatos.dinheiro(item.getSubtotal())).append('\n');
        }
        if (!materiais.isEmpty()) {
            sb.append("\nMateriais:\n");
            for (ItemMaterial im : materiais) {
                sb.append("  ").append(Formatos.numero(im.getQuantidade())).append(' ').append(im.getUnidade())
                  .append(" ").append(im.getDescricaoMaterial())
                  .append(" = ").append(Formatos.dinheiro(im.getSubtotal())).append('\n');
            }
        }
        if (!Formatos.vazio(obs)) {
            sb.append("\nObs.: ").append(obs).append('\n');
        }
        sb.append("\nVALOR TOTAL: ").append(Formatos.dinheiro(calcularValorTotal()));
        return sb.toString();
    }

    @Override
    public String toString() {
        return "OS " + id + " - " + getNomeCliente() + " - " + Formatos.dinheiro(calcularValorTotal());
    }

    public static List<OrdemServico> listar() {
        return new ArrayList<>(BancoDados.get().getOrdens());
    }

    public static List<OrdemServico> listarPorStatus(char status) {
        List<OrdemServico> lista = new ArrayList<>();
        for (OrdemServico os : BancoDados.get().getOrdens()) {
            if (os.status == status) {
                lista.add(os);
            }
        }
        return lista;
    }

    public static OrdemServico buscar(int id) {
        return BancoDados.buscar(BancoDados.get().getOrdens(), id);
    }

    public char getStatus() {
        return status;
    }

    /** Uso interno (recebimento finaliza/reabre a ordem). */
    void setStatus(char status) {
        this.status = status;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public LocalDate getDataAgendamento() {
        return dataAgendamento;
    }

    public void setDataAgendamento(LocalDate dataAgendamento) {
        this.dataAgendamento = dataAgendamento;
    }

    public LocalTime getHoraAgendamento() {
        return horaAgendamento;
    }

    public void setHoraAgendamento(LocalTime horaAgendamento) {
        this.horaAgendamento = horaAgendamento;
    }

    public LocalDate getDataPrevistaEntrega() {
        return dataPrevistaEntrega;
    }

    public void setDataPrevistaEntrega(LocalDate dataPrevistaEntrega) {
        this.dataPrevistaEntrega = dataPrevistaEntrega;
    }

    public LocalDate getDataEntrega() {
        return dataEntrega;
    }

    void setDataEntrega(LocalDate dataEntrega) {
        this.dataEntrega = dataEntrega;
    }

    public String getRetiradaPor() {
        return retiradaPor;
    }

    void setRetiradaPor(String retiradaPor) {
        this.retiradaPor = retiradaPor;
    }

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }

    /** Valor total gravado no último salvar(). */
    public double getValorTotal() {
        return valorTotal;
    }

    public List<ItemOrdemServico> getItens() {
        return itens;
    }

    public void setItens(List<ItemOrdemServico> itens) {
        this.itens = new ArrayList<>(itens);
    }

    public List<ItemMaterial> getMateriais() {
        return materiais;
    }

    public void setMateriais(List<ItemMaterial> materiais) {
        this.materiais = new ArrayList<>(materiais);
    }
}
