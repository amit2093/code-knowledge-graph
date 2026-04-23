package com.ckg;

import com.ckg.service.GraphIngestionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CkgApplication {

    public static void main(String[] args) {
        SpringApplication.run(CkgApplication.class, args);
    }

    @Bean
    CommandLineRunner initializeGraph(
            GraphIngestionService ingestionService,
            @Value("${analyzer.target.path}") String targetPath) {
        return args -> {
            // Blocks startup until JGraphT is fully populated
            ingestionService.ingestProject(targetPath);
            System.out.println(">>> IN-MEMORY GRAPH POPULATED: " + targetPath);
        };
    }
}
