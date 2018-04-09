package com.lab.mq.mqdemo;

import com.lab.mq.mqdemo.message.Dog;
import com.lab.mq.mqdemo.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableRabbit
@EnableAspectJAutoProxy(exposeProxy = true)
public class MqDemoApplication implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MqDemoApplication.class);

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(MqDemoApplication.class, args);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private DemoService demoService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        //        demoService.addScore();
        for (int i = 0; i < 10000; i++) {
            rabbitTemplate.convertAndSend("animal", "animal.bark", new Dog("旺财" + i, "white" + i));
        }
    }

}
