package com.wsk.dao;

import com.wsk.pojo.OrderForm;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderFormMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(OrderForm record);

    int insertSelective(OrderForm record);

    OrderForm selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(OrderForm record);

    int updateByPrimaryKey(OrderForm record);

    int getCounts(int uid);

    List<OrderForm> selectByUid(@Param("uid") int uid, @Param("start") int start);
}