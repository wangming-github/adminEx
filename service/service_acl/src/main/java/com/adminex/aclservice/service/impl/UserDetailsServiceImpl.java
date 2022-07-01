package com.adminex.aclservice.service.impl;

import com.adminex.aclservice.entity.User;
import com.adminex.aclservice.service.PermissionService;
import com.adminex.aclservice.service.UserService;
import com.adminex.common.security.entity.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author maizi
 */
@Slf4j
@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        //根据用户名查询数据
        User user = userService.selectByUsername(username);
        //判断
        if (user == null) {
            log.info("2.查询校验用户{}，用户不存在", username);
            throw new UsernameNotFoundException("用户不存在");
        }
        log.info("2.DB查询校验用户{}", username);


        //创建security内部对象
        com.adminex.common.security.entity.User curUser = new com.adminex.common.security.entity.User();
        BeanUtils.copyProperties(user, curUser);

        //根据用户查询用户权限列表
        List<String> permissionValueList = permissionService.selectPermissionValueByUserId(user.getId());
        SecurityUser securityUser = new SecurityUser();
        securityUser.setCurrentUserInfo(curUser);
        securityUser.setPermissionValueList(permissionValueList);
        log.info("3.DB查询权限赋予给UserDetails对象返回{}", permissionValueList);
        return securityUser;
    }
}
