package work.eanson.configuraton;

import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import work.eanson.service.realm.LoginRealm;

import javax.annotation.Resource;


/**
 * Long live freedom and fraternity, No 996
 * <pre>
 *     整合shiro+shiro-redis
 *  用手机号作为sessionID
 *  文档:http://alexxiyang.github.io/shiro-redis/
 * </pre>
 *
 * @author eanson
 * @date 2020/6/14
 */
@Configuration
public class ShiroRedisConfig {

//    /**
//     * 注入RedisSessionDAO
//     */
//    @Autowired
//    RedisSessionDAO redisSessionDAO;
//
//    @Autowired
//    RedisCacheManager redisCacheManager;

    @Resource
    private JedisPool jedisPool;

    @Bean
    LoginRealm loginRealm() {
        return new LoginRealm();
    }


    @Bean
    public RedisManager redisManager() {
        RedisManager redisManager = new RedisManager();
        redisManager.setJedisPool(jedisPool);
//        redisManager.setHost("localhost:6379");
        return redisManager;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisManager redisManager) {
        RedisCacheManager cacheManager = new RedisCacheManager();
        cacheManager.setRedisManager(redisManager);
        return cacheManager;
    }

    @Bean
    public RedisSessionDAO redisSessionDAO(RedisManager redisManager) {
        RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setRedisManager(redisManager);
        return redisSessionDAO;
    }

    @Bean
    public SessionDAO sessionDAO(RedisSessionDAO redisSessionDAO) {
        return redisSessionDAO;
    }
    @Bean
    public SessionManager sessionManager(SessionDAO redisSessionDAO) {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();

        // inject redisSessionDAO
        sessionManager.setSessionDAO(redisSessionDAO);
        return sessionManager;
    }

    @Bean
    DefaultWebSecurityManager securityManager(RedisCacheManager cacheManager, SessionDAO sessionDAO) {
        DefaultWebSecurityManager defaultWebSecurityManager = new DefaultWebSecurityManager();
        defaultWebSecurityManager.setRealm(loginRealm());

        //inject sessionManager
        defaultWebSecurityManager.setSessionManager(sessionManager(sessionDAO));

//        用手机号作为session ID
        cacheManager.setPrincipalIdFieldName("telephone");

        // inject redisCacheManager
        defaultWebSecurityManager.setCacheManager(cacheManager);
        return defaultWebSecurityManager;
    }


    @Bean
    ShiroFilterChainDefinition shiroFilterChainDefinition() {
        return new DefaultShiroFilterChainDefinition();
    }

    /**
     * 解决spring aop和注解配置一起使用的bug。如果您在使用shiro注解配置的同时，引入了spring aop的starter，
     * 会有一个奇怪的问题，导致shiro注解的请求，不能被映射，需加入以下配置：
     *
     * @return
     */
    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        /**
         * setUsePrefix(false)用于解决一个奇怪的bug。在引入spring aop的情况下。
         * 在@Controller注解的类的方法中加入@RequiresRole等shiro注解，会导致该方法无法映射请求，导致返回404。
         * 加入这项配置能解决这个bug
         */
        defaultAdvisorAutoProxyCreator.setUsePrefix(true);
        return defaultAdvisorAutoProxyCreator;
    }
}
