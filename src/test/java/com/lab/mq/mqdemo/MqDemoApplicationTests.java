package com.lab.mq.mqdemo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MqDemoApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(MqDemoApplicationTests.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void test() throws InterruptedException {
        Thread producer = new Thread(() -> {
            String msg;
            for (int i = 0; i < 100; i++) {
                msg = "tomcat " + i;
                sned(msg);
                logger.info("send ====> {}", msg);
            }
        });
        producer.setName("producer");

        producer.start();
        Thread.sleep(100000);
    }

    public void sned(String msg) {
        rabbitTemplate.convertAndSend("animal", "animal.smart", msg);
    }

    public void receive() {
        Message message = rabbitTemplate.receive("direct-queue-demo");
        System.out.println("received ===> " + message);
    }


}
