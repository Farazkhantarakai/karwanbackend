package com.aiecomm.camp.modules.template.ServiceImpl;

import com.aiecomm.camp.modules.template.Service.TemplateService;
import com.aiecomm.camp.modules.template.dto.TemplateDto;
import com.aiecomm.camp.modules.template.entity.Template;
import com.aiecomm.camp.modules.template.repository.TemplateRepository;
import org.springframework.stereotype.Service;

@Service
public class TemplateServiceImpl implements TemplateService {

    private TemplateRepository templateRepository;

    TemplateServiceImpl(TemplateRepository templateRepository){
        this.templateRepository=templateRepository;
    }

    @Override
    public void saveTemplateFromAdmin(TemplateDto templateDto) throws Exception {

        Template template= Template.builder().templateName(templateDto.getTemplateName()).templateSetting(templateDto.getTemplateSetting()).build();
        templateRepository.save(template);

    }
}
