package com.wsk.dao;

import com.wsk.pojo.UserCollection;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserCollectionMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserCollection record);

    int insertSelective(UserCollection record);

    UserCollection selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(UserCollection record);

    int updateByPrimaryKey(UserCollection record);

    int getCounts(int uid);

    List<UserCollection> selectByUid(@Param("uid") int uid, @Param("start") int start);

    UserCollection selectByUidAndSid(@Param("uid") int uid, @Param("sid") int sid);
}