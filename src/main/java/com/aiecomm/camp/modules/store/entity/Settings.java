package com.aiecomm.camp.modules.store.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "store")
@EqualsAndHashCode(exclude = "store")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "country_currency")
    private String countryCurrency;

    @Column(name = "country_language")
    private String countryLanguage;
}
