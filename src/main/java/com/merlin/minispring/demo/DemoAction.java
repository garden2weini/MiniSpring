package com.merlin.minispring.demo;

import com.merlin.minispring.mvcframework.annotation.MLAutowired;
import com.merlin.minispring.mvcframework.annotation.MLController;
import com.merlin.minispring.mvcframework.annotation.MLRequestMapping;
import com.merlin.minispring.mvcframework.annotation.MLRequestParam;
import com.merlin.minispring.demo.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MLController
@MLRequestMapping("/demo")
public class DemoAction {
    @MLAutowired
    private IDemoService demoService;

    @MLRequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp, @MLRequestParam("name") String name) {
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MLRequestMapping("/add.json")
    public void add(HttpServletRequest req, HttpServletResponse resp, @MLRequestParam("a") Integer a, @MLRequestParam("b") Integer b) {
        try {
            if(null == a) a = -1;
            if(null == b) b = -1;
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @MLRequestMapping("/remove.json")
    public void remove(HttpServletRequest req, HttpServletResponse resp, @MLRequestParam("id") Integer id) {
        try {
            resp.getWriter().write("OK?............."+ id);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
