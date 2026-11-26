package com.wsk.dao;

import com.wsk.pojo.WantContext;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WantContextMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(WantContext record);

    int insertSelective(WantContext record);

    WantContext selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(WantContext record);

    int updateByPrimaryKey(WantContext record);

    List<WantContext> selectByUWid(@Param("uwid") int uwid, @Param("start") int start);

    int getCounts(int uwid);
}