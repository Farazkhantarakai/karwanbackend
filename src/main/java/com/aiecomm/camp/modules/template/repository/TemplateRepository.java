package com.aiecomm.camp.modules.template.repository;

import com.aiecomm.camp.modules.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository< Template,Long> {
}
