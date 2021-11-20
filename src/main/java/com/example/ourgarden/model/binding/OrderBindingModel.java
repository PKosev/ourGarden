package com.example.ourgarden.model.binding;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

public class OrderBindingModel {
    private BigDecimal quantity;
    private String comment;

    public OrderBindingModel() {

    }

    @NotNull
    @Positive
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
