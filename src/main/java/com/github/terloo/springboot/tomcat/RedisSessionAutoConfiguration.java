package com.github.terloo.springboot.tomcat;

import com.github.terloo.springboot.tomcat.session.redis.DefaultKeyGenerator;
import com.github.terloo.springboot.tomcat.session.redis.RedisSession;
import com.github.terloo.springboot.tomcat.session.redis.RedisSessionFactoryCustomizer;
import com.github.terloo.springboot.tomcat.session.redis.RedisSessionKeyGenerator;
import com.github.terloo.springboot.tomcat.session.redis.RedisSessionManager;

import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ Tomcat.class, UpgradeProtocol.class })
public class RedisSessionAutoConfiguration {

    @Bean
    public RedisSessionFactoryCustomizer sessionRedisFactoryCustomizer(RedisSessionManager redisSessionManager) {
        return new RedisSessionFactoryCustomizer(redisSessionManager);
    }

    @Bean
    @ConditionalOnMissingBean(RedisSessionKeyGenerator.class)
    public RedisSessionKeyGenerator redisSessionKeyGenerator() {
        return new DefaultKeyGenerator();
    }

    @Bean
    public RedisSessionManager redisSessionManager(RedisTemplate<String, RedisSession> tomcatSessionRedisTemplate,
            RedisSessionKeyGenerator redisSessionKeyGenerator) {
        return new RedisSessionManager(tomcatSessionRedisTemplate, redisSessionKeyGenerator);
    }

    @Bean
    public RedisTemplate<String, RedisSession> tomcatSessionRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, RedisSession> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        JdkSerializationRedisSerializer sessionSerializer = new JdkSerializationRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(sessionSerializer);
        template.setHashValueSerializer(sessionSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
