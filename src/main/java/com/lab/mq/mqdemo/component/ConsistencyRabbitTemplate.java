package com.lab.mq.mqdemo.component;

import com.google.common.collect.Lists;
import com.lab.mq.mqdemo.entity.RabbitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Created by zhangwanli on 2017/11/6.
 */
public class ConsistencyRabbitTemplate extends RabbitTemplate {

    private static final Logger logger = LoggerFactory.getLogger(ConsistencyRabbitTemplate.class);

    private final ConsistentMessageHandler consistentMessageHandler;

    private static final ThreadLocal<RabbitMessage> CURRENT_MSG = new ThreadLocal<>();

    public ConsistencyRabbitTemplate(ConnectionFactory connectionFactory, ConsistentMessageHandler consistentMessageHandler) {
        super(connectionFactory);
        this.consistentMessageHandler = consistentMessageHandler;
    }

    public void send(final String exchange, final String routingKey, final Message message, CorrelationData correlationData) throws AmqpException {
        if (correlationData == null) {
            correlationData = new CorrelationData(UUID.randomUUID().toString());
        }
        //消息入库
        RabbitMessage msg = new RabbitMessage();
        msg.setId(correlationData.getId());
        msg.setExchange(exchange);
        msg.setRoutingKey(routingKey);
        msg.setMessage(new String(message.getBody()));
        CURRENT_MSG.set(msg);
        consistentMessageHandler.consistentPersist(msg);
        TransactionSynchronizationUtils.invokeAfterCommit(Lists.newArrayList(new TransactionSynchronizationAdapter() {


            @Override
            public void afterCommit() {
                //入库成功后异步发送
                RabbitMessage msg = CURRENT_MSG.get();
                CompletableFuture.runAsync(() -> {
                    try {
                        MessageProperties properties = new MessageProperties();
                        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        ConsistencyRabbitTemplate.super.send(msg.getExchange(), msg.getRoutingKey(), new Message(msg.getMessage().getBytes(),
                                properties), new CorrelationData(msg.getId()));
                    } catch (AmqpException e) {
                        logger.error("send message to rabbit error: {}", e.getMessage());
                    }
                });
            }

            @Override
            public void afterCompletion(int status) {
                super.afterCompletion(status);
            }

        }));

    }

}
