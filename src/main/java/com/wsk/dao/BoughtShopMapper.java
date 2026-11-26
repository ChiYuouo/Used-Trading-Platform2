package com.wsk.dao;

import com.wsk.pojo.BoughtShop;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BoughtShopMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(BoughtShop record);

    int insertSelective(BoughtShop record);

    BoughtShop selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(BoughtShop record);

    int updateByPrimaryKey(BoughtShop record);

    int getCounts(int uid);

    List<BoughtShop> selectByUid(@Param("uid") int uid, @Param("start") int start);
}