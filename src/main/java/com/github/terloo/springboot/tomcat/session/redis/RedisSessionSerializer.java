package com.github.terloo.springboot.tomcat.session.redis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class RedisSessionSerializer implements RedisSerializer<RedisSession> {

    @Autowired
    private RedisSessionManager redisSessionManager;

    @Override
    public byte[] serialize(RedisSession session) throws SerializationException {
        if (session == null) {
            return new byte[0];
        }

        byte[] serialized;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));) {
            session.writeObjectData(oos);
            oos.flush();
            serialized = bos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException(e.getMessage(), e);
        }

        return serialized;
    }

    @Override
    public RedisSession deserialize(byte[] data) throws SerializationException {
        if (data == null) {
            return null;
        }
        
        RedisSession session = new RedisSession(null);
        try (BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
                ObjectInputStream ois = new ObjectInputStream(bis);) {
            session.setManager(redisSessionManager);
            session.readObjectData(ois);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException(e.getMessage(), e);
        }
        return session;
    }

}
