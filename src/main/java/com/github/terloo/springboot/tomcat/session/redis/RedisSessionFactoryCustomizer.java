package com.github.terloo.springboot.tomcat.session.redis;

import org.apache.catalina.Context;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

public class RedisSessionFactoryCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private RedisSessionManager redisSessionManager;

    public RedisSessionFactoryCustomizer(RedisSessionManager redisSessionManager) {
        this.redisSessionManager = redisSessionManager;
    }

    @Override
    public void customize(TomcatServletWebServerFactory tomcatServletWebServerFactory) {
        tomcatServletWebServerFactory.addContextValves(new RedisSessionHandlerValve());
        tomcatServletWebServerFactory.addContextCustomizers(new TomcatContextCustomizer() {

            @Override
            public void customize(Context context) {
                context.setManager(redisSessionManager);
            }

        });
    }

}
