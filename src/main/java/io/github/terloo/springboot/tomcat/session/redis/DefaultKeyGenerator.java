package io.github.terloo.springboot.tomcat.session.redis;

public class DefaultKeyGenerator implements RedisSessionKeyGenerator {

    @Override
    public String generateKey(RedisSessionManager redisSessionManager, String requestedSessionId) {
        if (requestedSessionId == null) {
            requestedSessionId = redisSessionManager.generateDefaultKey();
        }
        return sessionIdWithJvmRoute(requestedSessionId, redisSessionManager.getJvmRoute());
    }

    private String sessionIdWithJvmRoute(String sessionId, String jvmRoute) {
        if (jvmRoute != null) {
            String jvmRoutePrefix = '.' + jvmRoute;
            return sessionId.endsWith(jvmRoutePrefix) ? sessionId : sessionId + jvmRoutePrefix;
        }
        return sessionId;
    }
    
}
