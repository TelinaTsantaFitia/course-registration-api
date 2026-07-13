package com.telina.demo.concurrency;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConf {

  public static final String IMAGE_PROCESSING_EXECUTOR = "imageProcessingExecutor";

  @Bean(name = IMAGE_PROCESSING_EXECUTOR)
  public Executor imageProcessingExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("img-async-");
    executor.initialize();
    return executor;
  }
}
