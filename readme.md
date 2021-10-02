# SpringBoot-tomcat-session-redis

一个SpringBoot-starter小工具，修改自[tomcat-redis-session-manager](https://github.com/jcoleman/tomcat-redis-session-manager)使其适配Tomcat8以及spring-boot-starter-data-redis。

**仍处于开发阶段**

## Future
1. 将SpringBoot内嵌tomcat的session存储至redis。
2. 与SpringSession通过Filter实现不同，该项目通过配置内嵌tomcat的方式来修改session存储位置。能较好兼容使用tomcat的filter进行session操作的第三方项目。

## 依赖
1. 环境：你的应用需要依赖web和redis的starter
   ```xml
   <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
   ```
2. 配置：你的应用需要redis相关配置
   ```properties
    spring.redis.host=127.0.0.1
    spring.redis.port=6379
    spring.redis.password=
    # 以及其他redis相关配置
   ```

## 使用方法
1. 引用starter依赖：
   ```xml
   <dependency>
        <groupId>com.github.terloo</groupId>
        <artifactId>embedded-tomcat-session-redis-boot-starter</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
   ```
2. 或者复制src至自己的项目中自行修改

## 配置项
1. 生成redis键值的规则，默认采用tomcat的session键值生成规则，示例中修改为uuid
   ```java
   import com.github.terloo.springboot.tomcat.session.redis.RedisSessionKeyGenerator;
   import com.github.terloo.springboot.tomcat.session.redis.RedisSessionManager;
   
   @Component
   public class MyGenerator implements RedisSessionKeyGenerator {

        @Override
        public String generateKey(RedisSessionManager redisSessionManager, String requestedSessionId) {
            return UUID.randomUUID().toString();
        }
        
   }
   ```