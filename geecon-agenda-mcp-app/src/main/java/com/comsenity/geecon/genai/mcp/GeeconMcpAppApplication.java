package com.comsenity.geecon.genai.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GeeconMcpAppApplication {

    static void main(String[] args) {
        SpringApplication.run(GeeconMcpAppApplication.class, args);
    }
}
