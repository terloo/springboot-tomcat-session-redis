package com.github.terloo.springboot.tomcat.session.redis;

public interface RedisSessionKeyGenerator {

    String generateKey(RedisSessionManager redisSessionManager, String requestedSessionId);

}
