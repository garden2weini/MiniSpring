package com.merlin.minispring.demo.service;

import com.merlin.minispring.mvcframework.annotation.MLService;

@MLService
public class DemoService implements IDemoService {
    public String get(String name) {
        return "My name is " + name;
    }
}
