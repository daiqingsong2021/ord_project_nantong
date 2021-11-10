package com.wisdom.acm.activiti.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.nacos.client.naming.net.HttpClient;
import com.wisdom.acm.activiti.form.TodoBusinessForm;
import com.wisdom.acm.activiti.service.ActSsoService;
import org.apache.velocity.util.ArrayListWrapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.*;

public class ActSsoServiceImpl implements ActSsoService {
    private String baseUrl = "http://58.221.217.2:17003";

    @Override
    public int insertTodoMsg() {
        List<TodoBusinessForm> list = new ArrayList<>();
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(factory);
        JSONArray json = new JSONArray(Collections.singletonList(list));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json;charset=UTF-8");
        HttpEntity<String> requestEntity = new HttpEntity<>(json.toString(), headers);
        String responseEntity = restTemplate.postForObject("/portal/union-todo-api/todoMsg", requestEntity, String.class);
        return 0;
    }

    @Override
    public int insertTodoBusiness() {
        List<TodoBusinessForm> list = new ArrayList<>();
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(factory);
        JSONArray json = new JSONArray(Collections.singletonList(list));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json;charset=UTF-8");
        HttpEntity<String> requestEntity = new HttpEntity<>(json.toString(), headers);
        String responseEntity = restTemplate.postForObject("/portal/union-todo-api/todoBusiness", requestEntity, String.class);
        return 0;
    }
}
