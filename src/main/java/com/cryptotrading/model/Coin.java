package com.cryptotrading.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coin {

    @Id
    private String  id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String name;

    private String image;

    private BigDecimal currentPrice;


    private BigDecimal marketCap;

    private Long marketCapRank;

    private BigDecimal fullyDilutedValuation;

    private BigDecimal totalVolume;

    private BigDecimal high24h;

    private BigDecimal low24h;

    private BigDecimal priceChange24h;

    private BigDecimal priceChangePercentage24h;

    private BigDecimal marketCapChange24h;

    private BigDecimal marketCapChangePercentage24h;

    private BigDecimal circulatingSupply;

    private BigDecimal totalSupply;

    private BigDecimal maxSupply;

    private BigDecimal ath;

    private BigDecimal athChangePercentage;

    private Instant athDate;

    private BigDecimal atl;

    private BigDecimal atlChangePercentage;

    private Instant atlDate;

    @JsonIgnore
    private String roi;

    private Instant lastUpdated;
}
