package com.aiecomm.camp.modules.template.Service;

import com.aiecomm.camp.modules.template.dto.TemplateDto;

public interface TemplateService {
    void saveTemplateFromAdmin(TemplateDto templateDto) throws Exception;
}
