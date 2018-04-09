package com.lab.mq.mqdemo.configuration;

import com.lab.mq.mqdemo.component.ConsistencyRabbitTemplate;
import com.lab.mq.mqdemo.component.ConsistentMessageHandler;
import com.lab.mq.mqdemo.component.DBConsistentMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;

import javax.sql.DataSource;

/**
 * Created by zhangwanli on 2017/11/3.
 */
@Configuration
@EnableConfigurationProperties({RabbitProperties.class})
public class RabbitConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RabbitConfiguration.class);

    @Autowired
    private ObjectProvider<MessageConverter> messageConverter;

    @Autowired
    private RabbitProperties properties;


    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        initRabbitTemplate(rabbitTemplate);
        return rabbitTemplate;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnClass({DataSource.class, JdbcTemplate.class})
    public ConsistencyRabbitTemplate consistencyRabbitTemplate(ConnectionFactory connectionFactory, JdbcTemplate jdbcTemplate) {
        ConsistentMessageHandler consistentMessageHandler = consistentMessageHandler(jdbcTemplate);
        ConsistencyRabbitTemplate template = new ConsistencyRabbitTemplate(connectionFactory, consistentMessageHandler);
        initRabbitTemplate(template);
        template.setConfirmCallback(consistentMessageHandler::onConfirm);
        return template;
    }

    private void initRabbitTemplate(RabbitTemplate rabbitTemplate) {
        MessageConverter messageConverter = this.messageConverter.getIfUnique();
        if (messageConverter != null) {
            rabbitTemplate.setMessageConverter(messageConverter);
        }
        rabbitTemplate.setMandatory(determineMandatoryFlag());
        RabbitProperties.Template templateProperties = this.properties.getTemplate();
        RabbitProperties.Retry retryProperties = templateProperties.getRetry();
        if (retryProperties.isEnabled()) {
            rabbitTemplate.setRetryTemplate(createRetryTemplate(retryProperties));
        }
        if (templateProperties.getReceiveTimeout() != null) {
            rabbitTemplate.setReceiveTimeout(templateProperties.getReceiveTimeout());
        }
        if (templateProperties.getReplyTimeout() != null) {
            rabbitTemplate.setReplyTimeout(templateProperties.getReplyTimeout());
        }
    }

    @Bean
    public ConsistentMessageHandler consistentMessageHandler(JdbcTemplate jdbcTemplate) {
        return new DBConsistentMessageHandler(jdbcTemplate);
    }

    private boolean determineMandatoryFlag() {
        Boolean mandatory = this.properties.getTemplate().getMandatory();
        return (mandatory != null ? mandatory : this.properties.isPublisherReturns());
    }

    private RetryTemplate createRetryTemplate(RabbitProperties.Retry properties) {
        RetryTemplate template = new RetryTemplate();
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(properties.getMaxAttempts());
        template.setRetryPolicy(policy);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getInitialInterval());
        backOffPolicy.setMultiplier(properties.getMultiplier());
        backOffPolicy.setMaxInterval(properties.getMaxInterval());
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setErrorHandler(errorHandler());
        return factory;
    }

    @Bean
    public Exchange animalExchange() {
        return new DirectExchange("animal", true, true);
    }

    @Bean
    public Queue catQueue() {
        return new Queue("cat", true);
    }

    @Bean
    public Queue dogQueue() {
        return new Queue("dog", true);
    }

    @Bean
    public Binding dogBinding() {
        return BindingBuilder.bind(dogQueue()).to(animalExchange()).with("animal.bark").noargs();
    }

    @Bean
    public Binding catBinding() {
        return BindingBuilder.bind(catQueue()).to(animalExchange()).with("animal.smart").noargs();
    }

    @Bean
    public ErrorHandler errorHandler() {
        return new ConditionalRejectingErrorHandler(new ConditionalRejectingErrorHandler.DefaultExceptionStrategy() {
            @Override
            public boolean isFatal(Throwable t) {
                return super.isFatal(t);
            }

            @Override
            protected boolean isUserCauseFatal(Throwable cause) {
                return super.isUserCauseFatal(cause);
            }
        });
    }

}
