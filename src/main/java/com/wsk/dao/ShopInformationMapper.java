package com.wsk.dao;

import com.wsk.pojo.ShopInformation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ShopInformationMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ShopInformation record);

    int insertSelective(ShopInformation record);

    ShopInformation selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ShopInformation record);

    int updateByPrimaryKey(ShopInformation record);

    List<ShopInformation> selectTen(Map map);

    List<ShopInformation> selectOffShelf(@Param("uid") Integer uid, @Param("start") Integer start);

    int getCountsOffShelf(Integer uid);

    int getCounts();

    Integer selectIdByImage(String image);

    List<ShopInformation> selectByName(String name);

    //通过分类选择
    @Select("select * from shopinformation where sort=#{sort} and display = 1 order by id desc limit 24")
    List<ShopInformation> selectBySort(int sort);

    //选择用户的发布
    @Select("select * from shopinformation where uid=#{uid} and display = 1 order by id desc limit 100")
    List<ShopInformation> selectUserReleaseByUid(int uid);

    @Select("select * from shopinformation where uid=#{uid} and display = 1 order by id desc limit #{start}, #{end}")
    List<ShopInformation> selectUserReleaseByUidPage(@Param("uid") int uid, @Param("start") int start, @Param("end") int end);

    @Select("select count(id) from shopinformation where uid=#{uid} and display = 1")
    int getUserReleaseCounts(int uid);
}