package com.lab.mq.mqdemo.listener;

import com.lab.mq.mqdemo.message.Dog;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by zhangwanli on 2017/11/2.
 */
@Component
public class MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);

    public MessageListener() {
        logger.info("listener init...");
    }

    @RabbitListener(queues = {"cat"})
    public void handleCatMessage(String msg, Message message, Channel channel) throws IOException {
        logger.info("got ====> " + msg);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        //        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
    }

    @RabbitListener(queues = {"dog"})
    public void handleDogMessage(Dog dog, Message message, Channel channel) throws IOException {
        logger.info("got ====> " + dog);
        try {
            doBusiness();
        } catch (BizException e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    private void doBusiness() {

    }

}
