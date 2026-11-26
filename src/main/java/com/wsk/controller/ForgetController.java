package com.wsk.controller;

import com.wsk.pojo.UserInformation;
import com.wsk.pojo.UserPassword;
import com.wsk.response.BaseResponse;
import com.wsk.service.UserInformationService;
import com.wsk.service.UserPasswordService;
import com.wsk.tool.SaveSession;
import com.wsk.tool.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wsk1103 on 2017/5/9.
 */
@RestController
public class ForgetController {

    private static final long CODE_EXPIRE_MS = 5 * 60 * 1000L;

    @Resource
    private UserPasswordService userPasswordService;
    @Resource
    private UserInformationService userInformationService;

    @RequestMapping(value = "checkCode.do", method = {RequestMethod.POST, RequestMethod.GET})
    public Map<String, Object> checkPhone(HttpServletRequest request, Model model,
                                          @RequestParam String code, @RequestParam String token,
                                          @RequestParam(required = false) String phone) {
        Map<String, Object> map = new HashMap<>();
        String name = request.getParameter("name");
        if (!StringUtils.getInstance().isNullOrEmpty(name)) {
            request.getSession().setAttribute("name", name.trim());
        }
        String checkCodeToken = (String) request.getSession().getAttribute("token");
        if (StringUtils.getInstance().isNullOrEmpty(checkCodeToken) || !checkCodeToken.equals(token)) {
            map.put("result", 0);
            map.put("msg", "页面已过期，请刷新后重试");
            return map;
        }
        String sessionPhone = (String) request.getSession().getAttribute("phone");
        if (StringUtils.getInstance().isNullOrEmpty(sessionPhone)) {
            map.put("result", 0);
            map.put("msg", "请先获取验证码");
            return map;
        }
        if (!StringUtils.getInstance().isNullOrEmpty(phone) && !sessionPhone.equals(phone.trim())) {
            map.put("result", 0);
            map.put("msg", "手机号码与获取验证码时不一致");
            return map;
        }
        //验证码错误或超时
        if (!checkCodePhone(code, request)) {
            map.put("result", 0);
            map.put("msg", "验证码错误或已过期");
            return map;
        }
        request.getSession().setAttribute("codeVerified", true);
        map.put("result", 1);
        map.put("msg", "验证成功");
        return map;
    }

    //更新密码
    @RequestMapping("updatePassword.do")
    public BaseResponse updatePassword(HttpServletRequest request, Model model,
                                       @RequestParam String password, @RequestParam String token) {
        //防止重复提交
        String updatePasswordToken = (String) request.getSession().getAttribute("token");
        if (StringUtils.getInstance().isNullOrEmpty(updatePasswordToken) || !updatePasswordToken.equals(token)) {
            return BaseResponse.fail("页面已过期，请刷新后重试");
        }
        Boolean codeVerified = (Boolean) request.getSession().getAttribute("codeVerified");
        if (codeVerified == null || !codeVerified) {
            return BaseResponse.fail("请先完成短信验证码校验");
        }
        if (StringUtils.getInstance().isNullOrEmpty(password) || password.length() < 6 || password.length() > 32) {
            return BaseResponse.fail("密码长度需在6到32位之间");
        }
        String realPhone = (String) request.getSession().getAttribute("phone");
        if (StringUtils.getInstance().isNullOrEmpty(realPhone)) {
            return BaseResponse.fail("会话已失效，请重新获取验证码");
        }
        String newPassword = StringUtils.getInstance().getMD5(password);
        int uid;
        try {
            uid = userInformationService.selectIdByPhone(realPhone);
            if (uid == 0) {
                return BaseResponse.fail("该手机号用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResponse.fail("查询用户失败");
        }
        UserPassword existing = userPasswordService.selectByUid(uid);
        int result;
        try {
            if (existing == null) {
                UserPassword userPassword = new UserPassword();
                userPassword.setUid(uid);
                userPassword.setModified(new Date());
                userPassword.setPassword(newPassword);
                result = userPasswordService.insertSelective(userPassword);
            } else {
                UserPassword userPassword = new UserPassword();
                userPassword.setId(existing.getId());
                userPassword.setUid(uid);
                userPassword.setModified(new Date());
                userPassword.setPassword(newPassword);
                result = userPasswordService.updateByPrimaryKeySelective(userPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResponse.fail("密码更新失败");
        }
        //更新失败
        if (result != 1) {
            return BaseResponse.fail("密码更新失败");
        }
        UserInformation userInformation = userInformationService.selectByPrimaryKey(uid);
        request.getSession().setAttribute("userInformation", userInformation);
        request.getSession().setAttribute("uid", uid);
        request.getSession().removeAttribute("codePhone");
        request.getSession().removeAttribute("codePhoneTime");
        request.getSession().removeAttribute("codeVerified");
        SaveSession.getInstance().save(realPhone, System.currentTimeMillis());
        return BaseResponse.success("密码重置成功");
    }

    //check the phone`s code
    private boolean checkCodePhone(String codePhone, HttpServletRequest request) {
        if (StringUtils.getInstance().isNullOrEmpty(codePhone)) {
            return false;
        }
        String trueCodePhone = (String) request.getSession().getAttribute("codePhone");
        Long codeTime = (Long) request.getSession().getAttribute("codePhoneTime");
        if (StringUtils.getInstance().isNullOrEmpty(trueCodePhone)) {
            return false;
        }
        if (codeTime != null && System.currentTimeMillis() - codeTime > CODE_EXPIRE_MS) {
            return false;
        }
        return codePhone.trim().equals(trueCodePhone);
    }
}
