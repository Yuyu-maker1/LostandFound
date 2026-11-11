package com.jincheng.lostandfound.dao;

import com.jincheng.lostandfound.pojo.Station;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StationMapper {
    List<Station> selectEnabled(@Param("keyword") String keyword);

    /** 全部启用车站（公开接口用，不受下拉框 20 条限制） */
    List<Station> selectAllEnabled();

    Station selectById(@Param("id") Integer id);

    Station selectByName(@Param("stationName") String stationName);

    int insert(Station station);
}
