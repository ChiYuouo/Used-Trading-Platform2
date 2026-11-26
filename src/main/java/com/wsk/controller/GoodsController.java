package com.wsk.controller;

import com.wsk.bean.ShopContextBean;
import com.wsk.bean.ShopInformationBean;
import com.wsk.bean.UserWantBean;
import com.wsk.pojo.*;
import com.wsk.service.*;
import com.wsk.token.TokenProccessor;
import com.wsk.tool.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wsk1103 on 2017/5/14.
 */
@Controller
public class GoodsController {
    @Resource
    private ShopInformationService shopInformationService;
    @Resource
    private ShopContextService shopContextService;
    @Resource
    private UserInformationService userInformationService;
    @Resource
    private SpecificeService specificeService;
    @Resource
    private ClassificationService classificationService;
    @Resource
    private AllKindsService allKindsService;
    @Resource
    private UserWantService userWantService;
    @Resource
    private UserCollectionService userCollectionService;

    //进入到发布商品页面
    @RequestMapping(value = "/publish_product.do", method = RequestMethod.GET)
    public String publish(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        //先判断用户有没有登录
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            //如果没有登录
            return "redirect:/login.do";
        } else {
            model.addAttribute("userInformation", userInformation);
        }
        //如果登录了，判断该用户有没有经过认证
        try {
            String realName = userInformation.getRealname();
            String sno = userInformation.getSno();
            String dormitory = userInformation.getDormitory();
            if (StringUtils.getInstance().isNullOrEmpty(realName) || StringUtils.getInstance().isNullOrEmpty(sno) || StringUtils.getInstance().isNullOrEmpty(dormitory)) {
                //没有
                redirectAttributes.addFlashAttribute("message", "请先认证真实信息（真实姓名、学号、宿舍号）");
                return "redirect:/personal_info.do";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login.do";
        }
        String error = request.getParameter("error");
        if (!StringUtils.getInstance().isNullOrEmpty(error)) {
            if ("1".equals(error)) {
                model.addAttribute("message", "表单已过期或重复提交，请重新填写提交");
            } else {
                model.addAttribute("message", error);
            }
        }
        String goodsToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("goodsToken", goodsToken);
        model.addAttribute("shopInformation", new ShopInformation());
        model.addAttribute("action", 1);
        model.addAttribute("token", goodsToken);
        return "page/publish_product";
    }

    //模糊查询商品
    @RequestMapping(value = "/findShopByName.do")
    public String findByName(HttpServletRequest request, Model model,
                             @RequestParam(defaultValue = "") String name) {
        try {
            if (StringUtils.getInstance().isNullOrEmpty(name == null ? "" : name.trim())) {
                return "redirect:/mall_page.do";
            }
            name = name.trim();
            List<ShopInformation> shopInformations = shopInformationService.selectByName(name);
            UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
            if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
                userInformation = new UserInformation();
            }
            model.addAttribute("userInformation", userInformation);
            List<ShopInformationBean> shopInformationBeans = new ArrayList<>();
            String sortName;
            for (ShopInformation shopInformation : shopInformations) {
                int sort = shopInformation.getSort();
                sortName = getSort(sort);
                ShopInformationBean shopInformationBean = new ShopInformationBean();
                shopInformationBean.setId(shopInformation.getId());
                shopInformationBean.setName(shopInformation.getName());
                shopInformationBean.setLevel(shopInformation.getLevel());
                shopInformationBean.setRemark(shopInformation.getRemark());
                shopInformationBean.setPrice(shopInformation.getPrice().doubleValue());
                shopInformationBean.setQuantity(shopInformation.getQuantity());
                shopInformationBean.setTransaction(shopInformation.getTransaction());
                shopInformationBean.setSort(sortName);
                shopInformationBean.setUid(shopInformation.getUid());
                shopInformationBean.setImage(StringUtils.getInstance().normalizeImagePath(shopInformation.getImage()));
                shopInformationBeans.add(shopInformationBean);
            }
            model.addAttribute("shopInformationBean", shopInformationBeans);
            model.addAttribute("searchKeyword", name);
            model.addAttribute("totalPages", 1);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/mall_page.do";
        }
        return "page/mall_page";
    }

    //进入查看商品详情
    @RequestMapping(value = "/selectById.do")
    public String selectById(@RequestParam int id,
                             HttpServletRequest request, Model model) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        boolean isLogin = !StringUtils.getInstance().isNullOrEmpty(userInformation);
        if (!isLogin) {
            userInformation = new UserInformation();
            model.addAttribute("userInformation", userInformation);
        }
        try {
            ShopInformation shopInformation = shopInformationService.selectByPrimaryKey(id);
            if (shopInformation == null) {
                return "redirect:/mall_page.do";
            }
            if (shopInformation.getDisplay() != null && shopInformation.getDisplay() == 0) {
                if (!isLogin || !userInformation.getId().equals(shopInformation.getUid())) {
                    return "redirect:/mall_page.do";
                }
            }
            shopInformation.setImage(StringUtils.getInstance().normalizeImagePath(shopInformation.getImage()));
            model.addAttribute("shopInformation", shopInformation);
            List<ShopContext> shopContexts = shopContextService.selectById(id);
            List<ShopContextBean> shopContextBeans = new ArrayList<>();
            for (ShopContext s : shopContexts) {
                ShopContextBean shopContextBean = new ShopContextBean();
                UserInformation u = userInformationService.selectByPrimaryKey(s.getUid());
                shopContextBean.setContext(s.getContext());
                shopContextBean.setId(s.getId());
                shopContextBean.setModified(s.getModified());
                shopContextBean.setUid(u != null ? u.getId() : s.getUid());
                shopContextBean.setUsername(u != null ? u.getUsername() : "匿名用户");
                shopContextBeans.add(shopContextBean);
            }
            boolean collected = false;
            Integer collectionId = null;
            if (isLogin && userInformation.getId() != null) {
                UserCollection uc = userCollectionService.selectByUidAndSid(userInformation.getId(), id);
                if (uc != null && uc.getDisplay() != null && uc.getDisplay() == 1) {
                    collected = true;
                    collectionId = uc.getId();
                }
            }
            String sort = getSort(shopInformation.getSort());
            String goodsToken = TokenProccessor.getInstance().makeToken();
            request.getSession().setAttribute("goodsToken", goodsToken);
            model.addAttribute("token", goodsToken);
            model.addAttribute("sort", sort);
            model.addAttribute("userInformation", userInformation);
            model.addAttribute("shopContextBeans", shopContextBeans);
            model.addAttribute("collected", collected);
            model.addAttribute("collectionId", collectionId);
            return "page/product_info";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    //进入到求购商城
    @RequestMapping(value = "/require_mall.do")
    public String requireMall(HttpServletRequest request, Model model,
                              @RequestParam(defaultValue = "1") int page) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (StringUtils.getInstance().isNullOrEmpty(userInformation)) {
            userInformation = new UserInformation();
        }
        model.addAttribute("userInformation", userInformation);
        if (page < 1) {
            page = 1;
        }
        int pageSize = 12;
        int totalCounts = userWantService.getAllCounts();
        int totalPages = Math.max(1, (totalCounts + pageSize - 1) / pageSize);
        if (page > totalPages) {
            page = totalPages;
        }
        List<UserWant> userWants = userWantService.selectPage((page - 1) * pageSize, pageSize);
        List<UserWantBean> list = convertUserWants(userWants);
        model.addAttribute("list", list);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCounts", totalCounts);
        return "page/require_mall";
    }

    //通过分类筛选求购信息
    @RequestMapping(value = "/selectWantBySort.do")
    @ResponseBody
    public List<UserWantBean> selectWantBySort(@RequestParam int sort) {
        return convertUserWants(userWantService.selectBySort(sort));
    }

    //求购商城分页查询
    @RequestMapping(value = "/selectWantByCounts.do")
    @ResponseBody
    public List<UserWantBean> selectWantByCounts(@RequestParam(defaultValue = "1") int counts) {
        if (counts < 1) {
            counts = 1;
        }
        return convertUserWants(userWantService.selectPage((counts - 1) * 12, 12));
    }

    //通过id查看商品的详情
    @RequestMapping(value = "/findShopById.do")
    @ResponseBody
    public ShopInformation findShopById(@RequestParam int id) {
        ShopInformation s = shopInformationService.selectByPrimaryKey(id);
        if (s != null) {
            s.setImage(StringUtils.getInstance().normalizeImagePath(s.getImage()));
        }
        return s;
    }

    //通过分类选择商品
    @RequestMapping(value = "/selectBySort.do")
    @ResponseBody
    public List<ShopInformation> selectBySort(@RequestParam int sort) {
        List<ShopInformation> list = shopInformationService.selectBySort(sort);
        for (ShopInformation s : list) {
            s.setImage(StringUtils.getInstance().normalizeImagePath(s.getImage()));
        }
        return list;
    }

    //分页查询
    @RequestMapping(value = "/selectByCounts.do")
    @ResponseBody
    public List<ShopInformation> selectByCounts(@RequestParam(defaultValue = "1") int counts) {
        if (counts < 1) {
            counts = 1;
        }
        Map<String, Integer> map = new HashMap<>();
        map.put("start", (counts - 1) * 12);
        map.put("end", 12);
        List<ShopInformation> list = shopInformationService.selectTen(map);
        for (ShopInformation s : list) {
            s.setImage(StringUtils.getInstance().normalizeImagePath(s.getImage()));
        }
        return list;
    }

    private List<UserWantBean> convertUserWants(List<UserWant> userWants) {
        List<UserWantBean> list = new ArrayList<>();
        if (userWants == null) {
            return list;
        }
        for (UserWant userWant : userWants) {
            UserWantBean u = new UserWantBean();
            u.setSort(getSort(userWant.getSort()));
            u.setRemark(userWant.getRemark());
            u.setQuantity(userWant.getQuantity());
            u.setPrice(userWant.getPrice().doubleValue());
            u.setUid(userWant.getUid());
            u.setId(userWant.getId());
            u.setModified(userWant.getModified());
            u.setName(userWant.getName());
            list.add(u);
        }
        return list;
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

}
