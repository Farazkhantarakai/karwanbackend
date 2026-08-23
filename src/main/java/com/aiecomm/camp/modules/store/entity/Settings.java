package com.aiecomm.camp.modules.store.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
//these two annotation are for preventing bidirectional mapping
@ToString(exclude = "store")
@EqualsAndHashCode(exclude = "store")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    @JsonBackReference
    private Store store;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "country_currency")
    private String countryCurrency;

    @Column(name = "country_language")
    private String countryLanguage;
}
