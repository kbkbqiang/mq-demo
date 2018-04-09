package com.lab.mq.mqdemo.service;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Created by zhangwanli on 2017/11/6.
 */
@Service
@Configuration
public class DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public Exchange userExchange() {
        return new DirectExchange("user", true, true);
    }

    @Bean
    public Queue scoreQueue() {
        return new Queue("score", true);
    }

    @Bean
    public Binding userBinding() {
        return BindingBuilder.bind(scoreQueue()).to(userExchange()).with("user.score").noargs();
    }

    @Transactional
    public void addScore() {
        String sql = "UPDATE `user` SET score = score + ?";
        jdbcTemplate.update(sql, 100);
        rabbitTemplate.convertAndSend("user", "user.score", "add 100 score");
    }

    @RabbitListener(queues = {"score"})
    public void handleScore(String content, Message message, Channel channel) throws IOException {
        logger.info("received score msg ====> {}", content);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
