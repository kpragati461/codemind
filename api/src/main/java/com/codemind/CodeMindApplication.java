package com.codemind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CodeMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeMindApplication.class, args);
    }
}
