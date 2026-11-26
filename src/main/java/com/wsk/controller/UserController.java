package com.wsk.controller;

import cn.hutool.core.io.FileUtil;
import com.wsk.bean.GoodsCarBean;
import com.wsk.bean.OrderFormBean;
import com.wsk.bean.ShopInformationBean;
import com.wsk.bean.UserCollectionBean;
import com.wsk.bean.UserWantBean;
import com.wsk.config.FileConfig;
import com.wsk.pojo.*;
import com.wsk.response.BaseResponse;
import com.wsk.service.*;
import com.wsk.token.TokenProccessor;
import com.wsk.tool.SaveSession;
import com.wsk.tool.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by wsk1103 on 2017/5/9.
 */
@Controller
@Slf4j
public class UserController {

    private static final Set<String> ALLOWED_IMAGE_EXT = new HashSet<>(
            Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp")
    );

    @Resource
    private UserInformationService userInformationService;
    @Resource
    private UserPasswordService userPasswordService;
    @Resource
    private UserCollectionService userCollectionService;
    @Resource
    private UserReleaseService userReleaseService;
    @Resource
    private BoughtShopService boughtShopService;
    @Resource
    private UserWantService userWantService;
    @Resource
    private ShopCarService shopCarService;
    @Resource
    private OrderFormService orderFormService;
    @Resource
    private GoodsOfOrderFormService goodsOfOrderFormService;
    @Resource
    private UserStateService userStateService;
    @Resource
    private ShopInformationService shopInformationService;
    @Resource
    private GoodsCarService goodsCarService;
    @Resource
    private SpecificeService specificeService;
    @Resource
    private ClassificationService classificationService;
    @Resource
    private AllKindsService allKindsService;
    @Resource
    private ShopContextService shopContextService;
    @Autowired
    private FileConfig fileConfig;

    //进入登录界面
    @RequestMapping(value = "/login.do", method = RequestMethod.GET)
    public String login(HttpServletRequest request, Model model) {
        String token = TokenProccessor.getInstance().makeToken();
        log.info("进入登录界面，token为:" + token);
        request.getSession().setAttribute("token", token);
        model.addAttribute("token", token);
        String msg = request.getParameter("msg");
        if (!StringUtils.getInstance().isNullOrEmpty(msg) && !model.containsAttribute("msg")) {
            model.addAttribute("msg", msg);
        }
        return "page/login_page";
    }

    //退出
    @RequestMapping(value = "/logout.do")
    public String logout(HttpServletRequest request) {
        try {
            request.getSession().removeAttribute("userInformation");
            request.getSession().removeAttribute("uid");
            log.info("用户退出登录");
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/home.do";
        }
        return "redirect:/";
    }

    //验证登录
    @RequestMapping(value = "/login.do", method = RequestMethod.POST)
    public String login(HttpServletRequest request,
                        @RequestParam String phone, @RequestParam String password, @RequestParam String token,
                        RedirectAttributes redirectAttributes) {
        String loginToken = (String) request.getSession().getAttribute("token");
        if (StringUtils.getInstance().isNullOrEmpty(phone) || StringUtils.getInstance().isNullOrEmpty(password)) {
            redirectAttributes.addFlashAttribute("msg", "请输入手机号码和密码");
            return "redirect:/login.do";
        }
        //防止重复提交
        if (StringUtils.getInstance().isNullOrEmpty(token) || !token.equals(loginToken)) {
            redirectAttributes.addFlashAttribute("msg", "登录页面已过期，请重新登录");
            return "redirect:/login.do";
        }
        boolean b = getId(phone.trim(), password, request);
        //失败，手机号码不存在或密码错误
        if (!b) {
            redirectAttributes.addFlashAttribute("msg", "手机号码或密码错误");
            return "redirect:/login.do";
        }
        request.getSession().removeAttribute("token");
        return "redirect:/";
    }

    //查看用户基本信息
    @RequestMapping(value = "/personal_info.do")
    public String personalInfo(HttpServletRequest request, Model model) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        // 从数据库刷新最新用户信息
        UserInformation latest = userInformationService.selectByPrimaryKey(userInformation.getId());
        if (latest != null) {
            userInformation = latest;
            request.getSession().setAttribute("userInformation", userInformation);
            request.getSession().setAttribute("uid", userInformation.getId());
        }
        String personalInfoToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("personalInfoToken", personalInfoToken);
        model.addAttribute("token", personalInfoToken);
        model.addAttribute("userInformation", userInformation);
        return "page/personal/personal_info";
    }

    //完善用户基本信息，认证
    @RequestMapping(value = "/certification.do", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> certification(HttpServletRequest request,
                                             @RequestParam(required = false) String userName,
                                             @RequestParam(required = false) String realName,
                                             @RequestParam(required = false) String clazz, @RequestParam String token,
                                             @RequestParam(required = false) String sno, @RequestParam(required = false) String dormitory,
                                             @RequestParam(required = false) String gender) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        Map<String, Object> map = new HashMap<>();
        map.put("result", 0);
        //该用户还没有登录
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            map.put("msg", "请先登录");
            return map;
        }
        String certificationToken = (String) request.getSession().getAttribute("personalInfoToken");
        if (StringUtils.getInstance().isNullOrEmpty(certificationToken) || !certificationToken.equals(token)) {
            map.put("msg", "页面已过期，请刷新后重试");
            return map;
        }
        if (userName != null) {
            userName = StringUtils.getInstance().replaceBlank(userName);
            if (userName.isEmpty() || userName.length() >= 25) {
                map.put("msg", "用户名长度需在1到24字符之间");
                return map;
            }
            userInformation.setUsername(userName);
        }
        if (realName != null) {
            realName = StringUtils.getInstance().replaceBlank(realName);
            if (realName.isEmpty() || realName.length() >= 25) {
                map.put("msg", "真实姓名长度需在1到24字符之间");
                return map;
            }
            userInformation.setRealname(realName);
        }
        if (clazz != null) {
            clazz = StringUtils.getInstance().replaceBlank(clazz);
            if (clazz.isEmpty() || clazz.length() >= 25) {
                map.put("msg", "班级长度需在1到24字符之间");
                return map;
            }
            userInformation.setClazz(clazz);
        }
        if (sno != null) {
            sno = StringUtils.getInstance().replaceBlank(sno);
            if (sno.isEmpty() || sno.length() > 12) {
                map.put("msg", "学号长度不能超过12位");
                return map;
            }
            userInformation.setSno(sno);
        }
        if (dormitory != null) {
            dormitory = StringUtils.getInstance().replaceBlank(dormitory);
            if (dormitory.isEmpty() || dormitory.length() >= 25) {
                map.put("msg", "宿舍号长度需在1到24字符之间");
                return map;
            }
            userInformation.setDormitory(dormitory);
        }
        if (gender != null) {
            gender = StringUtils.getInstance().replaceBlank(gender);
            if ("1".equals(gender)) {
                gender = "男";
            } else if ("2".equals(gender)) {
                gender = "女";
            }
            if (!"男".equals(gender) && !"女".equals(gender)) {
                map.put("msg", "性别格式不正确");
                return map;
            }
            userInformation.setGender(gender);
        }
        userInformation.setModified(new Date());
        int result = userInformationService.updateByPrimaryKeySelective(userInformation);
        if (result != 1) {
            //更新失败，认证失败
            map.put("msg", "更新失败，请稍后重试");
            return map;
        }
        //认证成功
        request.getSession().setAttribute("userInformation", userInformation);
        map.put("result", 1);
        map.put("msg", "更新成功");
        return map;
    }

    //已登录用户修改密码
    @RequestMapping(value = "/changePassword.do", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse changePassword(HttpServletRequest request,
                                       @RequestParam String oldPassword,
                                       @RequestParam String newPassword,
                                       @RequestParam String token) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "请先登录");
        }
        String personalInfoToken = (String) request.getSession().getAttribute("personalInfoToken");
        if (StringUtils.getInstance().isNullOrEmpty(personalInfoToken) || !personalInfoToken.equals(token)) {
            return BaseResponse.fail("页面已过期，请刷新后重试");
        }
        if (StringUtils.getInstance().isNullOrEmpty(oldPassword) || StringUtils.getInstance().isNullOrEmpty(newPassword)) {
            return BaseResponse.fail("请填写原密码和新密码");
        }
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            return BaseResponse.fail("新密码长度需在6到32位之间");
        }
        int uid = userInformation.getId();
        UserPassword userPassword = userPasswordService.selectByUid(uid);
        if (userPassword == null || !StringUtils.getInstance().getMD5(oldPassword).equals(userPassword.getPassword())) {
            return BaseResponse.fail("原密码不正确");
        }
        UserPassword update = new UserPassword();
        update.setId(userPassword.getId());
        update.setUid(uid);
        update.setPassword(StringUtils.getInstance().getMD5(newPassword));
        update.setModified(new Date());
        int res = userPasswordService.updateByPrimaryKeySelective(update);
        return res == 1 ? BaseResponse.success("密码修改成功") : BaseResponse.fail("密码修改失败");
    }

    //进入求购页面
    @RequestMapping(value = "/require_product.do")
    public String enterPublishUserWant(HttpServletRequest request, Model model) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        String error = request.getParameter("error");
        if (!StringUtils.getInstance().isNullOrEmpty(error)) {
            model.addAttribute("error", error);
        }
        String publishUserWantToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("publishUserWantToken", publishUserWantToken);
        model.addAttribute("token", publishUserWantToken);
        model.addAttribute("userInformation", userInformation);
        model.addAttribute("action", 1);
        return "page/require_product";
    }

    //修改求购商品
    @RequestMapping(value = "/modified_require_product.do")
    public String modifiedRequireProduct(HttpServletRequest request, Model model,
                                         @RequestParam int id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        UserWant userWant = userWantService.selectByPrimaryKey(id);
        if (userWant == null || !userInformation.getId().equals(userWant.getUid())) {
            return "redirect:/my_require_product.do";
        }
        String publishUserWantToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("publishUserWantToken", publishUserWantToken);
        model.addAttribute("token", publishUserWantToken);
        model.addAttribute("userInformation", userInformation);
        model.addAttribute("userWant", userWant);
        model.addAttribute("action", 2);
        String sort = getSort(userWant.getSort());
        model.addAttribute("sort", sort);
        return "page/modified_require_product";
    }

    //发布或修改求购（action=1 新增，action=2 修改）
    @RequestMapping(value = "/publishUserWant.do", method = RequestMethod.POST)
    public String publishUserWant(HttpServletRequest request, Model model,
                                  @RequestParam String name,
                                  @RequestParam int sort, @RequestParam int quantity,
                                  @RequestParam double price, @RequestParam String remark,
                                  @RequestParam String token,
                                  @RequestParam(required = false, defaultValue = "1") Integer action,
                                  @RequestParam(required = false, defaultValue = "0") Integer id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        String publishUserWantToke = (String) request.getSession().getAttribute("publishUserWantToken");
        if (StringUtils.getInstance().isNullOrEmpty(publishUserWantToke) || !publishUserWantToke.equals(token)) {
            return "redirect:/require_product.do?error=3";
        } else {
            request.getSession().removeAttribute("publishUserWantToken");
        }
        name = name == null ? "" : name.trim();
        remark = remark == null ? "" : remark.trim();
        if (name.isEmpty() || remark.isEmpty() || name.length() > 50 || remark.length() > 255 || quantity <= 0 || price <= 0) {
            return action != null && action == 2 && id != null && id > 0
                    ? "redirect:/modified_require_product.do?id=" + id + "&error=1"
                    : "redirect:/require_product.do?error=1";
        }
        name = StringUtils.getInstance().txtReplace(name);
        remark = StringUtils.getInstance().txtReplace(remark);
        int uid = userInformation.getId();
        if (action != null && action == 2 && id != null && id > 0) {
            UserWant existing = userWantService.selectByPrimaryKey(id);
            if (existing == null || !existing.getUid().equals(uid)) {
                return "redirect:/my_require_product.do";
            }
            UserWant update = new UserWant();
            update.setId(id);
            update.setModified(new Date());
            update.setName(name);
            update.setPrice(BigDecimal.valueOf(price));
            update.setQuantity(quantity);
            update.setRemark(remark);
            update.setSort(sort);
            try {
                userWantService.updateByPrimaryKeySelective(update);
            } catch (Exception e) {
                e.printStackTrace();
                return "redirect:/modified_require_product.do?id=" + id + "&error=2";
            }
            return "redirect:/my_require_product.do";
        }
        UserWant userWant = new UserWant();
        userWant.setModified(new Date());
        userWant.setDisplay(1);
        userWant.setName(name);
        userWant.setPrice(BigDecimal.valueOf(price));
        userWant.setQuantity(quantity);
        userWant.setRemark(remark);
        userWant.setUid(uid);
        userWant.setSort(sort);
        try {
            int result = userWantService.insertSelective(userWant);
            if (result != 1) {
                return "redirect:/require_product.do?error=2";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/require_product.do?error=2";
        }
        return "redirect:/my_require_product.do";
    }

    //查看我的求购（支持分页）
    @RequestMapping(value = {"/my_require_product.do", "/my_require_product_page.do"})
    public String getUserWant(HttpServletRequest request, Model model,
                              @RequestParam(defaultValue = "1") int page) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        try {
            int uid = userInformation.getId();
            if (page < 1) {
                page = 1;
            }
            int pageSize = 10;
            int totalCounts = getUserWantCounts(uid);
            int totalPages = Math.max(1, (totalCounts + pageSize - 1) / pageSize);
            if (page > totalPages) {
                page = totalPages;
            }
            List<UserWant> list = userWantService.selectByUid(uid, (page - 1) * pageSize);
            List<UserWantBean> userWantBeans = new ArrayList<>();
            for (UserWant userWant : list) {
                UserWantBean userWantBean = new UserWantBean();
                userWantBean.setId(userWant.getId());
                userWantBean.setModified(userWant.getModified());
                userWantBean.setName(userWant.getName());
                userWantBean.setPrice(userWant.getPrice().doubleValue());
                userWantBean.setUid(uid);
                userWantBean.setQuantity(userWant.getQuantity());
                userWantBean.setRemark(userWant.getRemark());
                userWantBean.setSort(getSort(userWant.getSort()));
                userWantBeans.add(userWantBean);
            }
            model.addAttribute("userWant", userWantBeans);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
        model.addAttribute("userInformation", userInformation);
        return "page/personal/my_require_product_page";
    }

    //查看求购总数
    @RequestMapping(value = "/getUserWantCounts.do")
    @ResponseBody
    public Map<String, Integer> getUserWantCounts(HttpServletRequest request, Model model) {
        Map<String, Integer> map = new HashMap<>();
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            map.put("counts", -1);
            return map;
        }
        try {
            int counts = getUserWantCounts(userInformation.getId());
            map.put("counts", counts);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("counts", -1);
        }
        return map;
    }

    //删除求购（含归属校验）
    @RequestMapping(value = "/deleteUserWant.do")
    public String deleteUserWant(HttpServletRequest request, @RequestParam int id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        UserWant existing = userWantService.selectByPrimaryKey(id);
        if (existing == null || !userInformation.getId().equals(existing.getUid())) {
            return "redirect:/my_require_product.do";
        }
        UserWant userWant = new UserWant();
        userWant.setId(id);
        userWant.setDisplay(0);
        userWant.setModified(new Date());
        try {
            userWantService.updateByPrimaryKeySelective(userWant);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/my_require_product.do";
    }

    //收藏商品
    @RequestMapping(value = "/addUserCollection.do")
    @ResponseBody
    public BaseResponse addUserCollection(HttpServletRequest request, @RequestParam int sid) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "您还未登录，请先登录");
        }
        ShopInformation shop = shopInformationService.selectByPrimaryKey(sid);
        if (shop == null || (shop.getDisplay() != null && shop.getDisplay() == 0)) {
            return BaseResponse.fail("商品不存在或已下架");
        }
        int uid = userInformation.getId();
        UserCollection existing = userCollectionService.selectByUidAndSid(uid, sid);
        if (existing != null) {
            if (existing.getDisplay() != null && existing.getDisplay() == 1) {
                return BaseResponse.success("已在收藏夹中", existing.getId());
            }
            existing.setDisplay(1);
            existing.setModified(new Date());
            int res = userCollectionService.updateByPrimaryKeySelective(existing);
            return res == 1 ? BaseResponse.success("收藏成功", existing.getId()) : BaseResponse.fail("收藏失败");
        }
        UserCollection userCollection = new UserCollection();
        userCollection.setModified(new Date());
        userCollection.setSid(sid);
        userCollection.setUid(uid);
        userCollection.setDisplay(1);
        int result = userCollectionService.insertSelective(userCollection);
        if (result != 1) {
            return BaseResponse.fail("收藏失败");
        }
        UserCollection saved = userCollectionService.selectByUidAndSid(uid, sid);
        return BaseResponse.success("收藏成功", saved != null ? saved.getId() : null);
    }

    //取消收藏（支持 ucid 或 sid，含归属校验）
    @RequestMapping(value = "/deleteUserCollection.do")
    @ResponseBody
    public BaseResponse deleteUserCollection(HttpServletRequest request,
                                             @RequestParam(required = false) Integer ucid,
                                             @RequestParam(required = false) Integer sid) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "您还未登录，请先登录");
        }
        int uid = userInformation.getId();
        UserCollection target = null;
        if (ucid != null && ucid > 0) {
            target = userCollectionService.selectByPrimaryKey(ucid);
        } else if (sid != null && sid > 0) {
            target = userCollectionService.selectByUidAndSid(uid, sid);
        }
        if (target == null || !target.getUid().equals(uid)) {
            return BaseResponse.fail("收藏记录不存在");
        }
        UserCollection userCollection = new UserCollection();
        userCollection.setId(target.getId());
        userCollection.setModified(new Date());
        userCollection.setDisplay(0);
        int result = userCollectionService.updateByPrimaryKeySelective(userCollection);
        if (result != 1) {
            return BaseResponse.fail("取消收藏失败");
        }
        return BaseResponse.success("已取消收藏");
    }

    //查看我的收藏
    @RequestMapping(value = "/my_collection.do")
    public String myCollection(HttpServletRequest request, Model model,
                               @RequestParam(defaultValue = "1") int page) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        int uid = userInformation.getId();
        if (page < 1) {
            page = 1;
        }
        int pageSize = 10;
        int totalCounts = Math.max(0, getCollectionCounts(uid));
        int totalPages = Math.max(1, (totalCounts + pageSize - 1) / pageSize);
        if (page > totalPages) {
            page = totalPages;
        }
        List<UserCollection> collections = userCollectionService.selectByUid(uid, (page - 1) * pageSize);
        List<UserCollectionBean> beans = new ArrayList<>();
        for (UserCollection uc : collections) {
            ShopInformation shop = shopInformationService.selectByPrimaryKey(uc.getSid());
            if (shop == null) {
                continue;
            }
            UserCollectionBean bean = new UserCollectionBean();
            bean.setId(uc.getId());
            bean.setSid(uc.getSid());
            bean.setUid(uid);
            bean.setModified(uc.getModified());
            bean.setName(shop.getName());
            bean.setPrice(shop.getPrice().doubleValue());
            bean.setLevel(shop.getLevel());
            bean.setQuantity(shop.getQuantity());
            bean.setRemark(shop.getRemark());
            bean.setSort(getSort(shop.getSort()));
            bean.setImage(StringUtils.getInstance().normalizeImagePath(shop.getImage()));
            bean.setDisplay(shop.getDisplay() == null ? 1 : shop.getDisplay());
            beans.add(bean);
        }
        model.addAttribute("userInformation", userInformation);
        model.addAttribute("list", beans);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "page/personal/my_collection_page";
    }

    //购物车数量
    @RequestMapping(value = "/getShopCarCounts.do")
    @ResponseBody
    public BaseResponse getShopCarCounts(HttpServletRequest request) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "请先登录");
        }
        int uid = userInformation.getId();
        int counts = goodsCarService.getCounts(uid);
        return BaseResponse.success("success", counts);
    }

    //查看购物车
    @RequestMapping(value = "/shopping_cart.do")
    public String selectShopCar(HttpServletRequest request, Model model) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        model.addAttribute("userInformation", userInformation);
        int uid = userInformation.getId();
        List<GoodsCar> goodsCars = goodsCarService.selectByUid(uid);
        List<GoodsCarBean> goodsCarBeans = new ArrayList<>();
        for (GoodsCar goodsCar : goodsCars) {
            ShopInformation shopInformation = shopInformationService.selectByPrimaryKey(goodsCar.getSid());
            if (shopInformation == null || (shopInformation.getDisplay() != null && shopInformation.getDisplay() == 0)) {
                continue;
            }
            GoodsCarBean goodsCarBean = new GoodsCarBean();
            goodsCarBean.setUid(goodsCar.getUid());
            goodsCarBean.setSid(goodsCar.getSid());
            goodsCarBean.setModified(goodsCar.getModified());
            goodsCarBean.setId(goodsCar.getId());
            goodsCarBean.setQuantity(goodsCar.getQuantity());
            goodsCarBean.setName(shopInformation.getName());
            goodsCarBean.setRemark(shopInformation.getRemark());
            goodsCarBean.setImage(StringUtils.getInstance().normalizeImagePath(shopInformation.getImage()));
            goodsCarBean.setPrice(shopInformation.getPrice().doubleValue());
            goodsCarBean.setSort(getSort(shopInformation.getSort()));
            goodsCarBean.setStock(shopInformation.getQuantity());
            goodsCarBeans.add(goodsCarBean);
        }
        model.addAttribute("list", goodsCarBeans);
        return "page/shopping_cart";
    }

    //添加到购物车
    @RequestMapping(value = "/insertGoodsCar.do")
    @ResponseBody
    public BaseResponse insertGoodsCar(HttpServletRequest request, @RequestParam int id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "您还未登录，请先登录");
        }
        ShopInformation shop = shopInformationService.selectByPrimaryKey(id);
        if (shop == null || (shop.getDisplay() != null && shop.getDisplay() == 0)) {
            return BaseResponse.fail("商品不存在或已下架");
        }
        if (shop.getQuantity() == null || shop.getQuantity() <= 0) {
            return BaseResponse.fail("商品库存不足");
        }
        int uid = userInformation.getId();
        GoodsCar existing = goodsCarService.selectByUidAndSid(uid, id);
        if (existing != null) {
            int nextQty = existing.getQuantity() + 1;
            if (nextQty > shop.getQuantity()) {
                return BaseResponse.fail("已达到该商品最大库存");
            }
            existing.setQuantity(nextQty);
            existing.setModified(new Date());
            int result = goodsCarService.updateByPrimaryKeySelective(existing);
            return result == 1 ? BaseResponse.success("加入购物车成功") : BaseResponse.fail("加入购物车失败");
        }
        GoodsCar goodsCar = new GoodsCar();
        goodsCar.setDisplay(1);
        goodsCar.setModified(new Date());
        goodsCar.setQuantity(1);
        goodsCar.setUid(uid);
        goodsCar.setSid(id);
        int result = goodsCarService.insertSelective(goodsCar);
        if (result != 1) {
            return BaseResponse.fail("加入购物车失败");
        }
        return BaseResponse.success("加入购物车成功");
    }

    //更新购物车商品数量
    @RequestMapping(value = "/updateGoodsCar.do", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse updateGoodsCar(HttpServletRequest request,
                                       @RequestParam int id,
                                       @RequestParam int quantity) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "您还未登录，请先登录");
        }
        if (quantity < 1) {
            return BaseResponse.fail("商品数量不能小于1");
        }
        GoodsCar existing = goodsCarService.selectByPrimaryKey(id);
        if (existing == null || !userInformation.getId().equals(existing.getUid()) || (existing.getDisplay() != null && existing.getDisplay() == 0)) {
            return BaseResponse.fail("购物车条目不存在");
        }
        ShopInformation shop = shopInformationService.selectByPrimaryKey(existing.getSid());
        if (shop == null || (shop.getDisplay() != null && shop.getDisplay() == 0)) {
            return BaseResponse.fail("商品已下架");
        }
        if (shop.getQuantity() != null && quantity > shop.getQuantity()) {
            return BaseResponse.fail("超出商品库存（当前库存：" + shop.getQuantity() + "）");
        }
        GoodsCar update = new GoodsCar();
        update.setId(id);
        update.setQuantity(quantity);
        update.setModified(new Date());
        int result = goodsCarService.updateByPrimaryKeySelective(update);
        return result == 1 ? BaseResponse.success("更新成功") : BaseResponse.fail("更新失败");
    }

    //删除购物车的商品
    @RequestMapping(value = "/deleteShopCar.do")
    @ResponseBody
    public BaseResponse deleteShopCar(HttpServletRequest request, @RequestParam int id, @RequestParam(required = false, defaultValue = "0") int sid) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "您还未登录，请先登录");
        }
        int uid = userInformation.getId();
        GoodsCar existing = goodsCarService.selectByPrimaryKey(id);
        if (existing == null || !existing.getUid().equals(uid)) {
            return BaseResponse.fail("购物车条目不存在");
        }
        GoodsCar goodsCar = new GoodsCar();
        goodsCar.setDisplay(0);
        goodsCar.setId(id);
        goodsCar.setModified(new Date());
        int result = goodsCarService.updateByPrimaryKeySelective(goodsCar);
        if (result != 1) {
            return BaseResponse.fail("删除失败");
        }
        return BaseResponse.success("删除成功");
    }

    //购物车结算生成订单
    @RequestMapping(value = "/submitOrder.do", method = RequestMethod.POST)
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse submitOrder(HttpServletRequest request,
                                    @RequestParam String cartIds,
                                    @RequestParam String address,
                                    @RequestParam(required = false, defaultValue = "") String context) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "您还未登录，请先登录");
        }
        if (StringUtils.getInstance().isNullOrEmpty(cartIds == null ? "" : cartIds.trim())) {
            return BaseResponse.fail("请选择需要结算的商品");
        }
        if (StringUtils.getInstance().isNullOrEmpty(address == null ? "" : address.trim())) {
            return BaseResponse.fail("请填写收货地址");
        }
        address = address.trim();
        if (address.length() > 255) {
            return BaseResponse.fail("收货地址过长");
        }
        context = context == null ? "" : context.trim();
        if (context.length() > 255) {
            context = context.substring(0, 255);
        }
        int uid = userInformation.getId();
        String[] idArr = cartIds.split(",");
        List<GoodsCar> selectedCars = new ArrayList<>();
        Map<Integer, ShopInformation> shopMap = new HashMap<>();
        for (String rawId : idArr) {
            String trimmed = rawId.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int carId;
            try {
                carId = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                return BaseResponse.fail("购物车参数格式错误");
            }
            GoodsCar car = goodsCarService.selectByPrimaryKey(carId);
            if (car == null || !car.getUid().equals(uid) || (car.getDisplay() != null && car.getDisplay() == 0)) {
                return BaseResponse.fail("所选购物车条目无效或已被删除");
            }
            ShopInformation shop = shopInformationService.selectByPrimaryKey(car.getSid());
            if (shop == null || (shop.getDisplay() != null && shop.getDisplay() == 0)) {
                return BaseResponse.fail("商品已下架，无法下单");
            }
            if (shop.getQuantity() == null || shop.getQuantity() < car.getQuantity()) {
                return BaseResponse.fail("商品《" + shop.getName() + "》库存不足");
            }
            selectedCars.add(car);
            shopMap.put(car.getId(), shop);
        }
        if (selectedCars.isEmpty()) {
            return BaseResponse.fail("请选择需要结算的商品");
        }
        Date now = new Date();
        OrderForm orderForm = new OrderForm();
        orderForm.setUid(uid);
        orderForm.setAddress(address);
        orderForm.setContext(context);
        orderForm.setDisplay(1);
        orderForm.setModified(now);
        int orderRes = orderFormService.insertSelective(orderForm);
        if (orderRes != 1 || orderForm.getId() == null) {
            throw new RuntimeException("订单创建失败");
        }
        int ofid = orderForm.getId();
        for (GoodsCar car : selectedCars) {
            ShopInformation shop = shopMap.get(car.getId());
            GoodsOfOrderForm item = new GoodsOfOrderForm();
            item.setOfid(ofid);
            item.setSid(car.getSid());
            item.setQuantity(car.getQuantity());
            item.setDisplay(1);
            item.setModified(now);
            goodsOfOrderFormService.insertSelective(item);

            BoughtShop bought = new BoughtShop();
            bought.setUid(uid);
            bought.setSid(car.getSid());
            bought.setQuantity(car.getQuantity());
            bought.setState(1);
            bought.setModified(now);
            boughtShopService.insertSelective(bought);

            ShopInformation shopUpdate = new ShopInformation();
            shopUpdate.setId(shop.getId());
            shopUpdate.setQuantity(Math.max(0, shop.getQuantity() - car.getQuantity()));
            shopUpdate.setSales((shop.getSales() == null ? 0 : shop.getSales()) + car.getQuantity());
            shopUpdate.setModified(now);
            shopInformationService.updateByPrimaryKeySelective(shopUpdate);

            GoodsCar carUpdate = new GoodsCar();
            carUpdate.setId(car.getId());
            carUpdate.setDisplay(0);
            carUpdate.setModified(now);
            goodsCarService.updateByPrimaryKeySelective(carUpdate);
        }
        return BaseResponse.success("下单成功", ofid);
    }

    //查看我的订单
    @RequestMapping(value = "/my_orders.do")
    public String myOrders(HttpServletRequest request, Model model,
                           @RequestParam(defaultValue = "1") int page) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        int uid = userInformation.getId();
        if (page < 1) {
            page = 1;
        }
        int pageSize = 10;
        int totalCounts = Math.max(0, getOrderFormCounts(uid));
        int totalPages = Math.max(1, (totalCounts + pageSize - 1) / pageSize);
        if (page > totalPages) {
            page = totalPages;
        }
        List<OrderForm> orderForms = orderFormService.selectByUid(uid, (page - 1) * pageSize);
        List<OrderFormBean> orderBeans = new ArrayList<>();
        for (OrderForm of : orderForms) {
            OrderFormBean bean = new OrderFormBean();
            bean.setId(of.getId());
            bean.setUid(of.getUid());
            bean.setModified(of.getModified());
            bean.setAddress(of.getAddress());
            bean.setContext(of.getContext());
            List<GoodsOfOrderForm> goodsList = goodsOfOrderFormService.selectByOFid(of.getId());
            List<OrderFormBean.OrderItemBean> items = new ArrayList<>();
            double total = 0.0;
            for (GoodsOfOrderForm gof : goodsList) {
                ShopInformation shop = shopInformationService.selectByPrimaryKey(gof.getSid());
                OrderFormBean.OrderItemBean item = new OrderFormBean.OrderItemBean();
                item.setId(gof.getId());
                item.setSid(gof.getSid());
                item.setQuantity(gof.getQuantity());
                if (shop != null) {
                    item.setName(shop.getName());
                    item.setImage(StringUtils.getInstance().normalizeImagePath(shop.getImage()));
                    item.setPrice(shop.getPrice().doubleValue());
                } else {
                    item.setName("已删除商品");
                    item.setImage("/img/home/feature_prodects/cont1.jpg");
                    item.setPrice(0.0);
                }
                double sub = item.getPrice() * item.getQuantity();
                item.setSubtotal(sub);
                total += sub;
                items.add(item);
            }
            bean.setItems(items);
            bean.setTotalPrice(Math.round(total * 100.0) / 100.0);
            orderBeans.add(bean);
        }
        model.addAttribute("userInformation", userInformation);
        model.addAttribute("orders", orderBeans);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "page/personal/my_orders_page";
    }

    //删除订单（软删除，含归属校验）
    @RequestMapping(value = "/deleteOrder.do", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse deleteOrder(HttpServletRequest request, @RequestParam int id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "请先登录");
        }
        OrderForm existing = orderFormService.selectByPrimaryKey(id);
        if (existing == null || !userInformation.getId().equals(existing.getUid())) {
            return BaseResponse.fail("订单不存在");
        }
        OrderForm update = new OrderForm();
        update.setId(id);
        update.setDisplay(0);
        update.setModified(new Date());
        int res = orderFormService.updateByPrimaryKeySelective(update);
        return res == 1 ? BaseResponse.success("订单已删除") : BaseResponse.fail("删除失败");
    }

    //发布或更新商品
    @RequestMapping(value = "/insertGoods.do", method = RequestMethod.POST)
    public String insertGoods(@RequestParam String name, @RequestParam int level,
                              @RequestParam String remark, @RequestParam double price,
                              @RequestParam int sort, @RequestParam int quantity,
                              @RequestParam String token, @RequestParam(required = false) MultipartFile image,
                              @RequestParam int action, @RequestParam(required = false, defaultValue = "0") Integer id,
                              HttpServletRequest request, Model model) {
        String goodsToken = (String) request.getSession().getAttribute("goodsToken");
        //防止重复提交
        if (StringUtils.getInstance().isNullOrEmpty(goodsToken) || !goodsToken.equals(token)) {
            return "redirect:/publish_product.do?error=1";
        } else {
            request.getSession().removeAttribute("goodsToken");
        }
        //从session中获得用户的基本信息
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        model.addAttribute("userInformation", userInformation);
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        name = StringUtils.getInstance().replaceBlank(name);
        remark = StringUtils.getInstance().replaceBlank(remark);
        //校验数据格式
        if (StringUtils.getInstance().isNullOrEmpty(name) || StringUtils.getInstance().isNullOrEmpty(remark)
                || level < 1 || level > 10 || price <= 0 || sort <= 0 || quantity <= 0
                || name.length() > 25 || remark.length() > 122) {
            model.addAttribute("message", "请输入正确的格式（商品名≤25字，详情≤122字，价格和数量大于0）");
            model.addAttribute("token", goodsToken);
            model.addAttribute("shopInformation", new ShopInformation());
            model.addAttribute("action", action);
            request.getSession().setAttribute("goodsToken", goodsToken);
            return "page/publish_product";
        }
        name = StringUtils.getInstance().txtReplace(name);
        remark = StringUtils.getInstance().txtReplace(remark);
        int uid = userInformation.getId();

        //插入
        if (action == 1) {
            if (image == null || image.isEmpty()) {
                model.addAttribute("message", "请选择图片");
                model.addAttribute("token", goodsToken);
                request.getSession().setAttribute("goodsToken", goodsToken);
                return "redirect:/publish_product.do?error=请选择图片";
            }
            String[] savedPaths = saveImageAndThumbnail(image);
            if (savedPaths == null) {
                request.getSession().setAttribute("goodsToken", goodsToken);
                return "redirect:/publish_product.do?error=图片保存或生成缩略图失败，请上传jpg/png格式图片";
            }
            String imageUrl = savedPaths[0];
            String thumbUrl = savedPaths[1];

            //写入商品表
            ShopInformation shopInformation = new ShopInformation();
            shopInformation.setName(name);
            shopInformation.setLevel(level);
            shopInformation.setRemark(remark);
            shopInformation.setPrice(BigDecimal.valueOf(price));
            shopInformation.setSort(sort);
            shopInformation.setQuantity(quantity);
            shopInformation.setDisplay(1);
            shopInformation.setTransaction(1);
            shopInformation.setSales(0);
            shopInformation.setModified(new Date());
            shopInformation.setImage(imageUrl);
            shopInformation.setThumbnails(thumbUrl);
            shopInformation.setUid(uid);
            try {
                int result = shopInformationService.insertSelective(shopInformation);
                if (result != 1) {
                    model.addAttribute("message", "商品保存失败，请检查输入格式");
                    model.addAttribute("token", goodsToken);
                    model.addAttribute("shopInformation", shopInformation);
                    model.addAttribute("action", 1);
                    request.getSession().setAttribute("goodsToken", goodsToken);
                    return "page/publish_product";
                }
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("token", goodsToken);
                model.addAttribute("message", "商品保存失败，请检查输入格式");
                model.addAttribute("shopInformation", shopInformation);
                model.addAttribute("action", 1);
                request.getSession().setAttribute("goodsToken", goodsToken);
                return "page/publish_product";
            }
            int sid = shopInformationService.selectIdByImage(imageUrl);
            if (sid == 0 && shopInformation.getId() != null) {
                sid = shopInformation.getId();
            }
            //将发布的商品编号插入到用户的发布记录中
            UserRelease userRelease = new UserRelease();
            userRelease.setModified(new Date());
            userRelease.setDisplay(1);
            userRelease.setSid(sid);
            userRelease.setUid(uid);
            try {
                int result = userReleaseService.insertSelective(userRelease);
                if (result != 1) {
                    shopInformationService.deleteByPrimaryKey(sid);
                    model.addAttribute("token", goodsToken);
                    model.addAttribute("message", "商品发布关联失败，请重试");
                    model.addAttribute("shopInformation", shopInformation);
                    model.addAttribute("action", 1);
                    request.getSession().setAttribute("goodsToken", goodsToken);
                    return "page/publish_product";
                }
            } catch (Exception e) {
                shopInformationService.deleteByPrimaryKey(sid);
                e.printStackTrace();
                model.addAttribute("token", goodsToken);
                model.addAttribute("message", "商品发布关联失败，请重试");
                model.addAttribute("shopInformation", shopInformation);
                model.addAttribute("action", 1);
                request.getSession().setAttribute("goodsToken", goodsToken);
                return "page/publish_product";
            }
            shopInformation.setId(sid);
            goodsToken = TokenProccessor.getInstance().makeToken();
            request.getSession().setAttribute("goodsToken", goodsToken);
            model.addAttribute("token", goodsToken);
            model.addAttribute("shopInformation", shopInformation);
            model.addAttribute("userInformation", userInformation);
            model.addAttribute("sort", getSort(sort));
            model.addAttribute("action", 2);
            return "redirect:/my_publish_product_page.do";
        } else if (action == 2) {
            //更新商品（含归属校验与可选图片更新）
            if (id == null || id <= 0) {
                return "redirect:/my_publish_product_page.do";
            }
            ShopInformation existing = shopInformationService.selectByPrimaryKey(id);
            if (existing == null || !existing.getUid().equals(uid)) {
                return "redirect:/my_publish_product_page.do";
            }
            ShopInformation shopInformation = new ShopInformation();
            shopInformation.setModified(new Date());
            shopInformation.setQuantity(quantity);
            shopInformation.setSort(sort);
            shopInformation.setPrice(BigDecimal.valueOf(price));
            shopInformation.setRemark(remark);
            shopInformation.setLevel(level);
            shopInformation.setName(name);
            shopInformation.setId(id);
            if (image != null && !image.isEmpty()) {
                String[] savedPaths = saveImageAndThumbnail(image);
                if (savedPaths != null) {
                    shopInformation.setImage(savedPaths[0]);
                    shopInformation.setThumbnails(savedPaths[1]);
                }
            }
            try {
                int result = shopInformationService.updateByPrimaryKeySelective(shopInformation);
                if (result != 1) {
                    return "redirect:/modifiedMyPublishProduct.do?id=" + id;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "redirect:/modifiedMyPublishProduct.do?id=" + id;
            }
            goodsToken = TokenProccessor.getInstance().makeToken();
            request.getSession().setAttribute("goodsToken", goodsToken);
            model.addAttribute("token", goodsToken);
            shopInformation = shopInformationService.selectByPrimaryKey(id);
            model.addAttribute("userInformation", userInformation);
            model.addAttribute("shopInformation", shopInformation);
            model.addAttribute("action", 2);
            model.addAttribute("sort", getSort(sort));
        }
        return "redirect:/my_publish_product_page.do";
    }

    //保存商品原图和缩略图，返回 [imageUrl, thumbUrl]
    private String[] saveImageAndThumbnail(MultipartFile image) {
        String originalFilename = image.getOriginalFilename();
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            String candidate = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!ALLOWED_IMAGE_EXT.contains(candidate)) {
                return null;
            }
            ext = candidate;
        }
        String fileName = StringUtils.getInstance().getRandomChar() + System.currentTimeMillis() + ext;
        String thumbName = StringUtils.getInstance().getRandomChar() + System.currentTimeMillis() + ".jpg";
        File imageDir = new File(fileConfig.getImageDirPath());
        File thumbDir = new File(fileConfig.getThumbnailsDirPath());
        FileUtil.mkdir(imageDir);
        FileUtil.mkdir(thumbDir);
        File imageFile = new File(imageDir, fileName);
        File thumbFile = new File(thumbDir, thumbName);
        try {
            image.transferTo(imageFile);
        } catch (Exception e) {
            log.error("保存商品图片失败", e);
            return null;
        }
        if (!StringUtils.getInstance().thumbnails(imageFile.getAbsolutePath(), thumbFile.getAbsolutePath())) {
            FileUtil.del(imageFile);
            return null;
        }
        String imageUrl = "/toImage/" + fileName;
        String thumbUrl = "/toImage/thumbnails/" + thumbName;
        return new String[]{imageUrl, thumbUrl};
    }

    //从发布的商品直接跳转到修改商品（含归属校验）
    @RequestMapping(value = "/modifiedMyPublishProduct.do")
    public String modifiedMyPublishProduct(HttpServletRequest request, Model model,
                                           @RequestParam int id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        }
        ShopInformation shopInformation = shopInformationService.selectByPrimaryKey(id);
        if (shopInformation == null || !userInformation.getId().equals(shopInformation.getUid())) {
            return "redirect:/my_publish_product_page.do";
        }
        shopInformation.setImage(StringUtils.getInstance().normalizeImagePath(shopInformation.getImage()));
        String goodsToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("goodsToken", goodsToken);
        model.addAttribute("token", goodsToken);
        model.addAttribute("userInformation", userInformation);
        model.addAttribute("shopInformation", shopInformation);
        model.addAttribute("action", 2);
        model.addAttribute("sort", getSort(shopInformation.getSort()));
        return "page/publish_product";
    }

    //发表留言
    @RequestMapping(value = "/insertShopContext.do")
    @ResponseBody
    public Map<String, Object> insertShopContext(@RequestParam int id, @RequestParam String context, @RequestParam String token,
                                                 HttpServletRequest request) {
        String goodsToken = (String) request.getSession().getAttribute("goodsToken");
        Map<String, Object> map = new HashMap<>();
        map.put("result", "0");
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            map.put("result", "2");
            map.put("msg", "请先登录");
            return map;
        }
        if (StringUtils.getInstance().isNullOrEmpty(goodsToken) || !token.equals(goodsToken)) {
            map.put("msg", "页面已过期，请刷新后重试");
            return map;
        }
        if (context == null || context.trim().isEmpty() || context.trim().length() > 255) {
            map.put("msg", "留言内容不能为空且不能超过255字");
            return map;
        }
        ShopInformation shop = shopInformationService.selectByPrimaryKey(id);
        if (shop == null) {
            map.put("msg", "商品不存在");
            return map;
        }
        String cleanContext = StringUtils.getInstance().txtReplace(context.trim());
        ShopContext shopContext = new ShopContext();
        shopContext.setContext(cleanContext);
        Date date = new Date();
        shopContext.setModified(date);
        shopContext.setSid(id);
        shopContext.setDisplay(1);
        int uid = userInformation.getId();
        shopContext.setUid(uid);
        try {
            int result = shopContextService.insertSelective(shopContext);
            if (result != 1) {
                map.put("msg", "发表留言失败");
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("msg", "发表留言失败");
            return map;
        }
        map.put("result", "1");
        map.put("username", userInformation.getUsername());
        map.put("context", cleanContext);
        map.put("time", StringUtils.getInstance().DateToString(date));
        return map;
    }

    //删除自己的商品留言
    @RequestMapping(value = "/deleteShopContext.do", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse deleteShopContext(HttpServletRequest request, @RequestParam int id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return BaseResponse.fail(2, "请先登录");
        }
        ShopContext existing = shopContextService.selectByPrimaryKey(id);
        if (existing == null || !userInformation.getId().equals(existing.getUid())) {
            return BaseResponse.fail("无权删除该留言");
        }
        ShopContext update = new ShopContext();
        update.setId(id);
        update.setDisplay(0);
        update.setModified(new Date());
        int res = shopContextService.updateByPrimaryKeySelective(update);
        return res == 1 ? BaseResponse.success("删除成功") : BaseResponse.fail("删除失败");
    }

    //下架商品（含归属校验）
    @RequestMapping(value = "/deleteShop.do")
    public String deleteShop(HttpServletRequest request, Model model, @RequestParam int id) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        } else {
            model.addAttribute("userInformation", userInformation);
        }
        ShopInformation existing = shopInformationService.selectByPrimaryKey(id);
        if (existing == null || !userInformation.getId().equals(existing.getUid())) {
            return "redirect:/my_publish_product_page.do";
        }
        ShopInformation shopInformation = new ShopInformation();
        shopInformation.setModified(new Date());
        shopInformation.setDisplay(0);
        shopInformation.setId(id);
        try {
            shopInformationService.updateByPrimaryKeySelective(shopInformation);
            return "redirect:/my_publish_product_page.do";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/my_publish_product_page.do";
        }
    }

    //查看发布的所有商品总数
    @RequestMapping(value = "/getReleaseShopCounts.do")
    @ResponseBody
    public Map<String, Integer> getReleaseShopCounts(HttpServletRequest request) {
        Map<String, Integer> map = new HashMap<>();
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            map.put("counts", -1);
            return map;
        }
        int counts = shopInformationService.getUserReleaseCounts(userInformation.getId());
        map.put("counts", counts);
        return map;
    }

    //查看我的发布的商品（支持分页）
    @RequestMapping(value = "/my_publish_product_page.do")
    public String getReleaseShop(HttpServletRequest request, Model model,
                                 @RequestParam(defaultValue = "1") int page) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            return "redirect:/login.do";
        } else {
            model.addAttribute("userInformation", userInformation);
        }
        int uid = userInformation.getId();
        if (page < 1) {
            page = 1;
        }
        int pageSize = 12;
        int totalCounts = shopInformationService.getUserReleaseCounts(uid);
        int totalPages = Math.max(1, (totalCounts + pageSize - 1) / pageSize);
        if (page > totalPages) {
            page = totalPages;
        }
        List<ShopInformation> shopInformations = shopInformationService.selectUserReleaseByUidPage(uid, (page - 1) * pageSize, pageSize);
        List<ShopInformationBean> list = new ArrayList<>();
        for (ShopInformation shopInformation : shopInformations) {
            String stringBuffer = getSort(shopInformation.getSort());
            ShopInformationBean shopInformationBean = new ShopInformationBean();
            shopInformationBean.setId(shopInformation.getId());
            shopInformationBean.setName(shopInformation.getName());
            shopInformationBean.setLevel(shopInformation.getLevel());
            shopInformationBean.setPrice(shopInformation.getPrice().doubleValue());
            shopInformationBean.setRemark(shopInformation.getRemark());
            shopInformationBean.setSort(stringBuffer);
            shopInformationBean.setQuantity(shopInformation.getQuantity());
            shopInformationBean.setTransaction(shopInformation.getTransaction());
            shopInformationBean.setUid(shopInformation.getUid());
            shopInformationBean.setImage(StringUtils.getInstance().normalizeImagePath(shopInformation.getImage()));
            list.add(shopInformationBean);
        }
        model.addAttribute("shopInformationBean", list);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "page/personal/my_publish_product_page";
    }

    private String getSort(int sort) {
        Specific specific = selectSpecificBySort(sort);
        if (specific == null) {
            return "其他";
        }
        Classification classification = selectClassificationByCid(specific.getCid());
        if (classification == null) {
            return specific.getName();
        }
        AllKinds allKinds = selectAllKindsByAid(classification.getAid());
        if (allKinds == null) {
            return classification.getName() + "-" + specific.getName();
        }
        return allKinds.getName() + "-" + classification.getName() + "-" + specific.getName();
    }

    //查看用户收藏的货物的总数
    private int getCollectionCounts(int uid) {
        try {
            return userCollectionService.getCounts(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    //查看用户的求购总个数
    private int getUserWantCounts(int uid) {
        try {
            return userWantService.getCounts(uid);
        } catch (Exception e) {
            return 0;
        }
    }

    //查看订单总数
    private int getOrderFormCounts(int uid) {
        try {
            return orderFormService.getCounts(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    //判断该手机号码及其密码是否一一对应
    private boolean getId(String phone, String password, HttpServletRequest request) {
        int uid = userInformationService.selectIdByPhone(phone);
        if (uid == 0 || StringUtils.getInstance().isNullOrEmpty(uid)) {
            return false;
        }
        UserInformation userInformation = userInformationService.selectByPrimaryKey(uid);
        if (null == userInformation) {
            return false;
        }
        password = StringUtils.getInstance().getMD5(password);
        UserPassword up = userPasswordService.selectByUid(userInformation.getId());
        if (up == null || up.getPassword() == null) {
            return false;
        }
        String password2 = up.getPassword();
        if (!password.equals(password2)) {
            return false;
        }
        //如果密码账号对应正确，将userInformation存储到session中
        request.getSession().setAttribute("userInformation", userInformation);
        request.getSession().setAttribute("uid", uid);
        SaveSession.getInstance().save(phone, System.currentTimeMillis());
        return true;
    }

    //获取最详细的分类，第三层
    private Specific selectSpecificBySort(int sort) {
        return specificeService.selectByPrimaryKey(sort);
    }

    //获得第二层分类
    private Classification selectClassificationByCid(int cid) {
        return classificationService.selectByPrimaryKey(cid);
    }

    //获得第一层分类
    private AllKinds selectAllKindsByAid(int aid) {
        return allKindsService.selectByPrimaryKey(aid);
    }

    public void save(ShopInformation shopInformation, UserRelease userRelease) {
        shopInformationService.insertSelective(shopInformation);
        userReleaseService.insertSelective(userRelease);
    }
}
