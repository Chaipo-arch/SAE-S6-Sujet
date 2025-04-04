package sae.semestre.six.domain.inventory.supplierInvoice;

import sae.semestre.six.domain.inventory.Inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "supplier_invoice_details")
public class SupplierInvoiceDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private SupplierInvoice supplierInvoice;
    
    @ManyToOne
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "unit_price")
    private Double unitPrice;
    
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
    
    public Integer getQuantity() {
        return quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }
}