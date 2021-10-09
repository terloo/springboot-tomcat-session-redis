# SpringBoot-tomcat-session-redis

一个SpringBoot-starter小工具，修改自[tomcat-redis-session-manager](https://github.com/jcoleman/tomcat-redis-session-manager)使其适配SpringBoot2.0+(Tomcat8.5+)。

**仍处于开发阶段**

## Future
1. 将SpringBoot内嵌tomcat的session存储至redis。
2. 与SpringSession通过Filter实现不同，该项目通过配置内嵌tomcat的Manager的方式来修改session存储位置。

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
1. 全局开关，默认为true
   ```properties
   server.servlet.session.store-in-redis=true
   ```
2. 生成redis键值的规则，默认采用tomcat的session键值生成规则，示例中修改为uuid
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
3. 持久化策略
   1. default：默认策略，只在识别到session改变时(**见注意事项**)，在请求结束后进行持久化。
   2. onChange：只要进行了set、delelte操作，立即持久化
   3. afterRequest：每次请求结束后都进行持久化
   > onChange和agterRequest策略可以同时存在，存在任一时default策略将会失效
   ```properties
   server.servlet.session.strategy=onChange,afterRequest
   ```

## 注意事项
1. 由于判断对象是否相等使用equals，导致如果隐式修改一个session，session将不会被识别为已改变，如下
   ```java
   List myArray = session.getAttribute("myArray");
   myArray.add(additionalArrayValue);
   ```