package com.adminex.filter;


import com.adminex.entity.SecurityUser;
import com.adminex.entity.User;
import com.adminex.security.TokenManager;
import com.adminex.utils.content;
import com.adminex.utils.utils.R;
import com.adminex.utils.utils.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录过滤器
 * <p>
 * 登录流程：
 * 用户登录会查询username、password以及权限列表
 * 然后会执行两步：
 * 1.根据key:username、value:password将登录信息存入redis
 * 2.根据username生成token
 * 生成的token会随着登录成功放在客户端cookie中
 * <p>
 * 授权流程：
 * 页面每次请求会将此token放在header中传递到后台
 * 从header中获取username
 * 根据username从redis中查询其权限列表
 * 然后由Spring-security给用户授权
 *
 * @author maizi
 */
public class TokenLoginFilter extends UsernamePasswordAuthenticationFilter {


    private TokenManager tokenManager;

    private RedisTemplate redisTemplate;

    /**
     * Spring封装好的权限管理工具类
     */
    private AuthenticationManager authenticationManager;

    /**
     * 构造方法注入对象
     */
    public TokenLoginFilter(AuthenticationManager authenticationManager, TokenManager tokenManager, RedisTemplate redisTemplate) {
        this.tokenManager = tokenManager;
        this.redisTemplate = redisTemplate;
        this.authenticationManager = authenticationManager;
        this.setPostOnly(false);
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login", "POST"));
    }

    /**
     * 获取表单提交的用户名密码
     *
     * @param request
     * @param response
     * @return
     * @throws AuthenticationException
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        //获取表单提交的数据
        try {
            //获取当前登录用户对象（ObjectMapper将流转换为对象）
            User user = new ObjectMapper().readValue(request.getInputStream(), User.class);

            UsernamePasswordAuthenticationToken authentication =
                    //先不赋值权限，只赋值用户名密码
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());

            //通过authenticationManager返回Authentication
            return authenticationManager.authenticate(authentication);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }


    /**
     * 认证成功后会执行此方法
     *
     * @param authResult 认证信息
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {
        //1.获取传递进来的认证信息 （此处可以进行强制类型转换是因为 SecurityUser implements UserDetails）
        SecurityUser securityUser = (SecurityUser) authResult.getPrincipal();
        //2.把用户名称和用户权限列表放到redis
        redisTemplate.opsForValue().set(securityUser.getCurrentUserInfo().getUsername(), securityUser.getPermissionValueList());
        //3.将认证信息生成token
        String jwtToken = tokenManager.createJwtToken(securityUser.getCurrentUserInfo().getUsername());
        //返回token(生成的token会随着登录成功放在客户端cookie中)
        ResponseUtil.out(response, R.ok().data(content.TOKEN, jwtToken));
    }

    /**
     * 认证失败调用的方法
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        ResponseUtil.out(response, R.error());
    }
}
