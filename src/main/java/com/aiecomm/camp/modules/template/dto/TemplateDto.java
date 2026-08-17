package com.aiecomm.camp.modules.template.dto;

import com.aiecomm.camp.modules.template.entity.Template;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateDto {


    private String templateName;
    private JsonNode templateSetting;

    public static TemplateDto fromEntity(Template entity) {
        if (entity == null) return null;
        return TemplateDto.builder()

                .templateName(entity.getTemplateName())
                .templateSetting(entity.getTemplateSetting())
                .build();
    }
}
