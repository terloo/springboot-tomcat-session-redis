package com.github.terloo.springboot.tomcat;

import com.github.terloo.springboot.tomcat.session.redis.RedisSession;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server.servlet.session")
public class RedisSessionProperties {

    private String strategy = "default";

    private DirtyTracking dirtyTracking;

    public String getStrategy() {
        return strategy;
    }

    public DirtyTracking getDirtyTracking() {
        return dirtyTracking;
    }

    public void setDirtyTracking(DirtyTracking dirtyTracking) {
        this.dirtyTracking = dirtyTracking;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public static class DirtyTracking {

        private Boolean enable = false;

        private String flag = "__changed__";

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            RedisSession.setManualDirtyTrackingSupportEnabled(enable);
            this.enable = enable;
        }

        public String getFlag() {
            return flag;
        }

        public void setFlag(String flag) {
            RedisSession.setManualDirtyTrackingAttributeKey(flag);
            this.flag = flag;
        }

    }

}
