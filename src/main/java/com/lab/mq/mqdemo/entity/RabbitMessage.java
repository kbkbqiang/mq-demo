package com.lab.mq.mqdemo.entity;

import com.lab.mq.mqdemo.enums.MessageStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.core.MessageProperties;

import java.util.Date;

/**
 * Created by zhangwanli on 2017/11/6.
 */
@Setter
@Getter
public class RabbitMessage {
    private String id;
    private String exchange;
    private String routingKey;
    private String message;
    private MessageProperties properties;
    private MessageStatus status = MessageStatus.WAITING_FOR_SEND;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
