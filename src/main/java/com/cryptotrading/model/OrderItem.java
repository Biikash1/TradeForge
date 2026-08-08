package com.cryptotrading.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Double quantity;

    @ManyToOne
    private Coin coin;

    private Double buyPrice;

    private Double sellPrice;

    @OneToOne
    @JsonIgnore
    private Order order;
}
