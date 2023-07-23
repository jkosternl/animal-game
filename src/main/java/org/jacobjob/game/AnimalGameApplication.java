package org.jacobjob.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AnimalGameApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnimalGameApplication.class, args);
  }
}
