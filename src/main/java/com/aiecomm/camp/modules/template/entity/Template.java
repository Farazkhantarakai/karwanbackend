package com.aiecomm.camp.modules.template.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;



@Entity
@Table(name = "templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_setting", columnDefinition = "jsonb")
    private JsonNode templateSetting;
}
