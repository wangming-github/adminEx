package com.adminex.security.config;

import com.adminex.security.filter.TokenAuthFilter;
import com.adminex.security.filter.TokenLoginFilter;
import com.adminex.security.security.DefaultPasswordEncoder;
import com.adminex.security.security.TokenLogoutHandler;
import com.adminex.security.security.TokenManager;
import com.adminex.security.security.UnauthEntryPoint;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 核心配置类
 *
 * @author maizi
 */


@Configuration
@EnableWebSecurity
@ComponentScan("com.adminex.security")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class TokenWebSecurityConfig extends WebSecurityConfigurerAdapter {

    private TokenManager tokenManager;
    private DefaultPasswordEncoder defaultPasswordEncoder;

    private RedisTemplate redisTemplate;
    private UserDetailsService userDetailsService;

    public TokenWebSecurityConfig(TokenManager tokenManager, DefaultPasswordEncoder defaultPasswordEncoder, RedisTemplate redisTemplate, UserDetailsService userDetailsService) {
        this.tokenManager = tokenManager;
        this.defaultPasswordEncoder = defaultPasswordEncoder;
        this.redisTemplate = redisTemplate;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 配置设置
     * 设置退出的地址和token，redis操作地址
     *
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable();

        //没有权限访问时调用自己指定的处理流程
        http.exceptionHandling().authenticationEntryPoint(new UnauthEntryPoint());

        //
        http.authorizeRequests().anyRequest().authenticated();

        //退出路径
        http.logout().logoutUrl("/logout").addLogoutHandler(new TokenLogoutHandler(tokenManager, redisTemplate));
        //添加登录过滤器
        http.addFilter(new TokenLoginFilter(authenticationManager(), tokenManager, redisTemplate));
        //添加授权过滤器
        http.addFilter(new TokenAuthFilter(authenticationManager(), tokenManager, redisTemplate));
        //httpBasic认证
        http.httpBasic();
    }

    /**
     * 调用userDetailsService和密码处理
     *
     * @param auth
     * @throws Exception
     */
    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                //调用自定义的MD5加密方法
                .passwordEncoder(defaultPasswordEncoder);
    }

    /**
     * 不进行认证的路径，可以直接访问
     *
     * @param web
     * @throws Exception
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/api/**");
    }
}

