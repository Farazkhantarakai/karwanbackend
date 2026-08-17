package com.aiecomm.camp.modules.template.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.modules.template.ServiceImpl.TemplateServiceImpl;
import com.aiecomm.camp.modules.template.dto.TemplateDto;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/template")
public class TemplateController {

@Autowired
    private  TemplateServiceImpl templateServiceimpl;
    Logger logger=LoggerFactory.getLogger(TemplateController.class);



    @PostMapping("/saveTemplateFromAdmin")
    public ResponseEntity<?> saveTemplate(
            @RequestBody TemplateDto templateDto
            ){
   try {
       this.templateServiceimpl.saveTemplateFromAdmin(templateDto);
       return ResponseEntity.ok().body(ApiResponse.builder().success(true).message("data saved successfully"));
   } catch (Exception e) {
      logger.error(e.getMessage());
      return ResponseEntity.internalServerError().build();
   }


    }



}
