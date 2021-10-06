package com.github.terloo.springboot.tomcat.session.redis;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.github.terloo.springboot.tomcat.RedisSessionProperties;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.session.ManagerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisSessionManager extends ManagerBase {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionManager.class);

    private RedisTemplate<String, RedisSession> tomcatSessionRedisTemplate;

    private RedisSessionKeyGenerator redisSessionKeyGenerator;

    @Autowired
    private RedisSessionProperties redisSessionProperties;

    public RedisSessionManager(RedisTemplate<String, RedisSession> tomcatSessionRedisTemplate,
            RedisSessionKeyGenerator redisSessionKeyGenerator) {
        this.tomcatSessionRedisTemplate = tomcatSessionRedisTemplate;
        this.redisSessionKeyGenerator = redisSessionKeyGenerator;
    }

    enum SessionPersistPolicy {
        DEFAULT, ON_CHANGE, AFTER_REQUEST;

        static SessionPersistPolicy fromName(String name) {
            for (SessionPersistPolicy policy : SessionPersistPolicy.values()) {
                if (policy.name().replaceAll("_", "").equalsIgnoreCase(name)) {
                    return policy;
                }
            }
            throw new IllegalArgumentException("Invalid session persist policy [" + name + "]. Must be one of "
                    + Arrays.asList(SessionPersistPolicy.values()) + ".");
        }
    }

    protected RedisSession NULL_SESSION = null;

    protected RedisSessionHandlerValve handlerValve;
    protected ThreadLocal<RedisSession> currentSession = new ThreadLocal<>();
    protected ThreadLocal<String> currentSessionId = new ThreadLocal<>();
    protected ThreadLocal<Boolean> currentSessionIsPersisted = new ThreadLocal<>();

    protected static String name = "RedisSessionManager";

    protected EnumSet<SessionPersistPolicy> sessionPersistPoliciesSet = EnumSet.of(SessionPersistPolicy.DEFAULT);

    @PostConstruct
    public void postConstruct() {
        String[] policyArray = redisSessionProperties.getStrategy().split(",");
        EnumSet<SessionPersistPolicy> policySet = EnumSet.of(SessionPersistPolicy.DEFAULT);
        for (String policyName : policyArray) {
            SessionPersistPolicy policy = SessionPersistPolicy.fromName(policyName);
            policySet.add(policy);
        }
        this.sessionPersistPoliciesSet = policySet;
    }

    public boolean getSaveOnChange() {
        return this.sessionPersistPoliciesSet.contains(SessionPersistPolicy.ON_CHANGE);
    }

    public boolean getAlwaysSaveAfterRequest() {
        return this.sessionPersistPoliciesSet.contains(SessionPersistPolicy.AFTER_REQUEST);
    }

    @Override
    public int getRejectedSessions() {
        // Essentially do nothing.
        return 0;
    }

    public void setRejectedSessions(int i) {
        // Do nothing.
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {

    }

    @Override
    public void unload() throws IOException {

    }

    /**
     * Start this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();

        setState(LifecycleState.STARTING);

        Boolean attachedToValve = false;
        for (Valve valve : getContext().getPipeline().getValves()) {
            if (valve instanceof RedisSessionHandlerValve) {
                this.handlerValve = (RedisSessionHandlerValve) valve;
                this.handlerValve.setRedisSessionManager(this);
                log.info("Attached to RedisSessionHandlerValve");
                attachedToValve = true;
                break;
            }
        }

        if (!attachedToValve) {
            String error = "Unable to attach to session handling valve; sessions cannot be saved after the request without the valve starting properly.";
            log.error(error);
            throw new LifecycleException(error);
        }

    }

    /**
     * Stop this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        if (log.isDebugEnabled()) {
            log.debug("Stopping");
        }

        setState(LifecycleState.STOPPING);

        // Require a new random number generator if we are restarted
        super.stopInternal();
    }

    @Override
    public Session createSession(String requestedSessionId) {
        RedisSession session = null;
        String sessionId = null;

        // Ensure generation of a unique session identifier.
        if (null != requestedSessionId) {
            sessionId = generateSessionId();
            if (!tomcatSessionRedisTemplate.opsForValue().setIfAbsent(sessionId, NULL_SESSION)) {
                sessionId = null;
            }
        } else {
            do {
                sessionId = generateSessionId();
            } while (!tomcatSessionRedisTemplate.opsForValue().setIfAbsent(sessionId, NULL_SESSION));
        }

        /* Even though the key is set in Redis, we are not going to flag
            the current thread as having had the session persisted since
            the session isn't actually serialized to Redis yet.
            This ensures that the save(session) at the end of the request
            will serialize the session into Redis with 'set' instead of 'setnx'. */

        if (null != sessionId) {
            session = (RedisSession) createEmptySession();
            session.setNew(true);
            session.setValid(true);
            session.setCreationTime(System.currentTimeMillis());
            session.setMaxInactiveInterval(getMaxInactiveInterval());
            session.setId(sessionId);
            session.tellNew();
        }

        currentSession.set(session);
        currentSessionId.set(sessionId);
        currentSessionIsPersisted.set(false);

        if (null != session) {
            try {
                saveInternal(session, true);
            } catch (IOException ex) {
                log.error("Error saving newly created session: " + ex.getMessage());
                currentSession.set(null);
                currentSessionId.set(null);
                session = null;
            }
        }

        return session;
    }

    @Override
    public Session createEmptySession() {
        return new RedisSession(this);
    }

    @Override
    public void add(Session session) {
        try {
            save(session);
        } catch (IOException ex) {
            log.warn("Unable to add to session manager store: " + ex.getMessage());
            throw new RuntimeException("Unable to add to session manager store.", ex);
        }
    }

    @Override
    public Session findSession(String id) throws IOException {
        RedisSession session = null;

        if (null == id) {
            currentSessionIsPersisted.set(false);
            currentSession.set(null);
            currentSessionId.set(null);
        } else if (id.equals(currentSessionId.get())) {
            session = currentSession.get();
        } else {
            session = loadSessionFromRedis(id);
            if (session != null) {
                currentSession.set(session);
                currentSessionIsPersisted.set(true);
                currentSessionId.set(id);
            } else {
                currentSessionIsPersisted.set(false);
                currentSession.set(null);
                currentSessionId.set(null);
            }
        }

        return session;
    }

    public RedisSession loadSessionFromRedis(String id) throws IOException {
        log.trace("Attempting to load session " + id + " from Redis");

        RedisSession session = tomcatSessionRedisTemplate.opsForValue().get(id);

        if (session == null) {
            log.trace("Session " + id + " not found in Redis");
        } else {
            session.setManager(this);
            session.setId(id);
            session.setNew(false);
            session.setMaxInactiveInterval(getMaxInactiveInterval());
            session.access();
            session.setValid(true);
            session.resetDirtyTracking();
        }

        return session;
    }

    public void save(Session session) throws IOException {
        save(session, false);
    }

    public void save(Session session, boolean forceSave) throws IOException {
        saveInternal(session, forceSave);
    }

    protected boolean saveInternal(Session session, boolean forceSave) throws IOException {
        log.trace("Saving session " + session + " into Redis");

        RedisSession redisSession = (RedisSession) session;

        if (log.isTraceEnabled()) {
            log.trace("Session Contents [" + redisSession.getId() + "]:");
            Enumeration<String> en = redisSession.getAttributeNames();
            while (en.hasMoreElements()) {
                log.trace("  " + en.nextElement());
            }
        }

        String sessionId = redisSession.getId();

        Boolean isCurrentSessionPersisted;
        if (forceSave || redisSession.isDirty()
                || null == (isCurrentSessionPersisted = this.currentSessionIsPersisted.get())
                || !isCurrentSessionPersisted) {

            log.trace("Save was determined to be necessary");

            tomcatSessionRedisTemplate.opsForValue().set(sessionId, redisSession);

            redisSession.resetDirtyTracking();
        } else {
            log.trace("Save was determined to be unnecessary");
        }

        log.trace("Setting expire timeout on session [" + redisSession.getId() + "] to " + getMaxInactiveInterval());
        tomcatSessionRedisTemplate.expire(sessionId, getMaxInactiveInterval(), TimeUnit.SECONDS);

        return true;
    }

    @Override
    public void remove(Session session) {
        remove(session, false);
    }

    @Override
    public void remove(Session session, boolean update) {
        log.trace("Removing session ID : " + session.getId());
        tomcatSessionRedisTemplate.delete(session.getId());
    }

    public void afterRequest() {
        RedisSession redisSession = currentSession.get();
        if (redisSession != null) {
            try {
                if (redisSession.isValid()) {
                    log.trace("Request with session completed, saving session " + redisSession.getId());
                    save(redisSession, getAlwaysSaveAfterRequest());
                } else {
                    log.trace("HTTP Session has been invalidated, removing :" + redisSession.getId());
                    remove(redisSession);
                }
            } catch (Exception e) {
                log.error("Error storing/removing session", e);
            } finally {
                currentSession.remove();
                currentSessionId.remove();
                currentSessionIsPersisted.remove();
                log.trace("Session removed from ThreadLocal :" + redisSession.getIdInternal());
            }
        }
    }

    public String generateSessionId() {
        return redisSessionKeyGenerator.generateKey(this, null);
    }

    public String generateDefaultKey() {
        return sessionIdGenerator.generateSessionId();
    }

    @Override
    public void processExpires() {
        // We are going to use Redis's ability to expire keys for session expiration.

        // Do nothing.
    }

    private int getMaxInactiveInterval() {
        if (getContext() != null) {
            return getContext().getSessionTimeout() * 60;
        }
        return -1;
    }

}
