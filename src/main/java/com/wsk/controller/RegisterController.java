package com.wsk.controller;

import com.wsk.pojo.UserInformation;
import com.wsk.pojo.UserPassword;
import com.wsk.pojo.UserState;
import com.wsk.response.BaseResponse;
import com.wsk.service.UserInformationService;
import com.wsk.service.UserPasswordService;
import com.wsk.service.UserStateService;
import com.wsk.tool.SaveSession;
import com.wsk.tool.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by wsk1103 on 2017/5/9.
 * 注册中心
 */
@Controller
public class RegisterController {
    @Resource
    private UserPasswordService userPasswordService;

    @Resource
    private UserInformationService userInformationService;

    @Resource
    private UserStateService userStateService;

    //开始注册用户
    @RequestMapping("/insertUser.do")
    @ResponseBody
    public BaseResponse insertUser(HttpServletRequest request,
                                   @RequestParam String password, @RequestParam String token) {
        //存储与session中的手机号码
        String realPhone = (String) request.getSession().getAttribute("phone");
        //token，唯一标识
        String insertUserToken = (String) request.getSession().getAttribute("token");
        //防止重复提交
        if (StringUtils.getInstance().isNullOrEmpty(insertUserToken) || !insertUserToken.equals(token)) {
            return BaseResponse.fail("页面已过期，请刷新后重试");
        }
        Boolean codeVerified = (Boolean) request.getSession().getAttribute("codeVerified");
        if (codeVerified == null || !codeVerified) {
            return BaseResponse.fail("请先完成短信验证码校验");
        }
        if (StringUtils.getInstance().isNullOrEmpty(realPhone)) {
            return BaseResponse.fail("手机号会话已失效，请重新获取验证码");
        }
        if (StringUtils.getInstance().isNullOrEmpty(password) || password.length() < 6 || password.length() > 32) {
            return BaseResponse.fail("密码长度需在6到32位之间");
        }
        //该手机号码已经存在
        int uid = userInformationService.selectIdByPhone(realPhone);
        if (uid != 0) {
            return BaseResponse.fail("该手机号码已被注册");
        }

        //用户信息
        UserInformation userInformation = new UserInformation();
        userInformation.setPhone(realPhone);
        userInformation.setCreatetime(new Date());
        String username = (String) request.getSession().getAttribute("name");
        if (StringUtils.getInstance().isNullOrEmpty(username)) {
            username = "用户" + (realPhone.length() >= 4 ? realPhone.substring(realPhone.length() - 4) : realPhone);
        }
        userInformation.setUsername(username.trim());
        userInformation.setModified(new Date());
        int result = userInformationService.insertSelective(userInformation);
        //如果用户基本信息写入成功
        if (result == 1) {
            uid = userInformationService.selectIdByPhone(realPhone);
            String newPassword = StringUtils.getInstance().getMD5(password);
            UserPassword userPassword = new UserPassword();
            userPassword.setModified(new Date());
            userPassword.setUid(uid);
            userPassword.setPassword(newPassword);
            result = userPasswordService.insertSelective(userPassword);
            //密码写入失败
            if (result != 1) {
                userInformationService.deleteByPrimaryKey(uid);
                return BaseResponse.fail("密码写入失败，请稍后重试");
            } else {
                //初始化用户状态记录
                try {
                    UserState userState = new UserState();
                    userState.setUid(uid);
                    userState.setCredit(80);
                    userState.setBalance(BigDecimal.ZERO);
                    userState.setModified(new Date());
                    userStateService.insertSelective(userState);
                } catch (Exception ignored) {
                }
                //注册成功，写入完整登录态
                userInformation = userInformationService.selectByPrimaryKey(uid);
                request.getSession().setAttribute("userInformation", userInformation);
                request.getSession().setAttribute("uid", uid);
                request.getSession().removeAttribute("codePhone");
                request.getSession().removeAttribute("codePhoneTime");
                request.getSession().removeAttribute("codeVerified");
                SaveSession.getInstance().save(realPhone, System.currentTimeMillis());
                return BaseResponse.success("注册成功");
            }
        }
        return BaseResponse.fail("注册失败，请稍后重试");
    }
}
