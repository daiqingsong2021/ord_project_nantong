package com.wisdom.acm.sys.config;

import com.wisdom.base.common.util.ListUtil;
import org.dozer.DozerBeanMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ListenerConfig {
    @Bean
    public DozerBeanMapper mapper() {
        DozerBeanMapper mapper = new DozerBeanMapper();
        // mapper.setMappingFiles(ListUtil.toArrayList("dozer/DefineDozerMapping.xml","dozer/PrepaDozerMapping.xml"));
        return mapper;
    }
}

