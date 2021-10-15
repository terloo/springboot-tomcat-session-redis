package io.github.terloo.springboot.tomcat;

import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.github.terloo.springboot.tomcat.session.redis.DefaultKeyGenerator;
import io.github.terloo.springboot.tomcat.session.redis.RedisSession;
import io.github.terloo.springboot.tomcat.session.redis.RedisSessionFactoryCustomizer;
import io.github.terloo.springboot.tomcat.session.redis.RedisSessionKeyGenerator;
import io.github.terloo.springboot.tomcat.session.redis.RedisSessionManager;
import io.github.terloo.springboot.tomcat.session.redis.RedisSessionSerializer;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "server.servlet.session.redis.enable", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({ Tomcat.class, UpgradeProtocol.class })
@EnableConfigurationProperties(RedisSessionProperties.class)
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
    public RedisSessionManager redisSessionManager() {
        return new RedisSessionManager();
    }

    @Bean
    public RedisSessionSerializer redisSessionSerializer() {
        return new RedisSessionSerializer();
    }

    @Bean
    public RedisTemplate<String, RedisSession> tomcatSessionRedisTemplate(RedisConnectionFactory factory, RedisSessionSerializer sessionSerializer) {
        RedisTemplate<String, RedisSession> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(sessionSerializer);
        template.setHashValueSerializer(sessionSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
