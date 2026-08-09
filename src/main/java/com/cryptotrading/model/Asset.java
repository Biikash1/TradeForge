package com.cryptotrading.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private double quantity;

    private double buyPrice;

    @ManyToOne
    private Coin coin;

    @ManyToOne
    private User user;
}
