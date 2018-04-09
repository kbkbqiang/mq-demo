package com.lab.mq.mqdemo.component;

import com.lab.mq.mqdemo.entity.RabbitMessage;
import com.lab.mq.mqdemo.enums.MessageStatus;
import org.springframework.amqp.rabbit.support.CorrelationData;

import java.util.List;

/**
 * 本地事务与mq消息发送的一致性处理。即本地事务成功，mq发送消息也需保证成功
 * Created by zhangwanli on 2017/11/6.
 */
public interface ConsistentMessageHandler {

    /**
     * 一致性存储消息。<p>
     * 发送mq之前与本地事务一致性存储待发送消息
     */
    void consistentPersist(RabbitMessage message);

    RabbitMessage queryById(String id);

    List<RabbitMessage> queryForSendList();

    void updateById(String id, MessageStatus status, String remark);

    void deleteById(String id);

    void deleteBatchByStatus(MessageStatus status);

    void onConfirm(CorrelationData correlationData, boolean ack, String cause);

}
