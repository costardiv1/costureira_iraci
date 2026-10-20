package costureira.model;

import java.io.Serializable;

/**
 * Item da ordem de serviço ("Itens serviço"): cada peça e o serviço que será feito nela.
 * Atributos: @id, *id_ordem, *id_servico, *descricao_peca, *quantidade,
 * @valor_unitario (copiado do valor padrão do serviço), **subtotal.
 *
 * É a classe que liga OrdemServico e Servico, resolvendo a relação N:N
 * (um cliente pede vários serviços e um serviço é pedido por vários clientes).
 */
public class ItemOrdemServico implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int idOrdem;
    private int idServico;
    private String nomeServico;
    private String descricaoPeca;
    private int quantidade = 1;
    private double valorUnitario;

    public ItemOrdemServico() {
    }

    /** Cria o item copiando o valor padrão atual do serviço (preço futuro não altera ordens antigas). */
    public ItemOrdemServico(Servico servico, String descricaoPeca, int quantidade) {
        this.idServico = servico.getId();
        this.nomeServico = servico.getNome();
        this.valorUnitario = servico.getValorPadrao();
        this.descricaoPeca = descricaoPeca;
        this.quantidade = quantidade;
    }

    /** Subtotal = quantidade × valor unitário. */
    public double getSubtotal() {
        return quantidade * valorUnitario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdOrdem() {
        return idOrdem;
    }

    public void setIdOrdem(int idOrdem) {
        this.idOrdem = idOrdem;
    }

    public int getIdServico() {
        return idServico;
    }

    public void setIdServico(int idServico) {
        this.idServico = idServico;
    }

    /** Nome do serviço no momento em que a ordem foi feita. */
    public String getNomeServico() {
        return nomeServico;
    }

    public void setNomeServico(String nomeServico) {
        this.nomeServico = nomeServico;
    }

    public String getDescricaoPeca() {
        return descricaoPeca;
    }

    public void setDescricaoPeca(String descricaoPeca) {
        this.descricaoPeca = descricaoPeca;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(double valorUnitario) {
        this.valorUnitario = valorUnitario;
    }
}
