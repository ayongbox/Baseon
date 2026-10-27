package io.github.ayongbox.baseon.demob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoBApplication {

    protected DemoBApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoBApplication.class, args);
    }
}
