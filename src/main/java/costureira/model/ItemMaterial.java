package costureira.model;

import java.io.Serializable;

/**
 * Material utilizado numa ordem de serviço (Servico — Materiais no diagrama de classes).
 * A quantidade usada sai do estoque do material e o valor é cobrado na ordem.
 * Atributos: @id, *id_ordem, *id_material, *quantidade, @valor_unitario, **subtotal.
 */
public class ItemMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int idOrdem;
    private int idMaterial;
    private String descricaoMaterial;
    private String unidade;
    private double quantidade;
    private double valorUnitario;

    public ItemMaterial() {
    }

    /** Cria o item copiando o valor atual do material. */
    public ItemMaterial(Material material, double quantidade) {
        this.idMaterial = material.getId();
        this.descricaoMaterial = material.getDescricao();
        this.unidade = material.getUnidade();
        this.valorUnitario = material.getValorProduto();
        this.quantidade = quantidade;
    }

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

    public int getIdMaterial() {
        return idMaterial;
    }

    public void setIdMaterial(int idMaterial) {
        this.idMaterial = idMaterial;
    }

    public String getDescricaoMaterial() {
        return descricaoMaterial;
    }

    public void setDescricaoMaterial(String descricaoMaterial) {
        this.descricaoMaterial = descricaoMaterial;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }

    public double getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(double valorUnitario) {
        this.valorUnitario = valorUnitario;
    }
}
