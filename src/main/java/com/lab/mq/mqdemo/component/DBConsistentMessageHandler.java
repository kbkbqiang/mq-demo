package com.lab.mq.mqdemo.component;

import com.lab.mq.mqdemo.entity.RabbitMessage;
import com.lab.mq.mqdemo.enums.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.aop.framework.AopContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by zhangwanli on 2017/11/6.
 */
public class DBConsistentMessageHandler implements ConsistentMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DBConsistentMessageHandler.class);

    private final JdbcTemplate jdbcTemplate;

    public DBConsistentMessageHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void consistentPersist(RabbitMessage message) {
        String sql = "INSERT INTO `t_mq_send` (`id`, `exchange`, `routing_key`, `message`, `status`, `remark`) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, message.getId(), message.getExchange(), message.getRoutingKey(), message.getMessage(), MessageStatus.WAITING_FOR_SEND.ordinal(), "");
    }

    @Override
    public RabbitMessage queryById(String id) {
        return null;
    }

    @Override
    public List<RabbitMessage> queryForSendList() {
        return null;
    }

    @Override
    @Transactional
    public void updateById(String id, MessageStatus status, String remark) {
        jdbcTemplate.update("UPDATE `t_mq_send` SET `status` = ? AND remark = ? WHERE id = ?", status.ordinal(), remark, id);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM `t_mq_send` WHERE id = ?", id);
    }

    @Override
    @Transactional
    public void deleteBatchByStatus(MessageStatus status) {
        jdbcTemplate.update("DELETE FROM `t_mq_send` WHERE `status` = ?", status.ordinal());
    }

    @Override
    @Transactional
    public void onConfirm(CorrelationData correlationData, boolean ack, String cause) {
        logger.info("publish confirm ====> id={}, ack={}, cause={}", correlationData.getId(), ack, cause);
        ConsistentMessageHandler proxy = (ConsistentMessageHandler) AopContext.currentProxy();
        if (ack) {
            proxy.deleteById(correlationData.getId());
        } else {
            proxy.updateById(correlationData.getId(), MessageStatus.SEND_ERROR, cause);
        }
    }

}
