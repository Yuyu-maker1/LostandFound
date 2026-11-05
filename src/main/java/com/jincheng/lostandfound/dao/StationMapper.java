package com.jincheng.lostandfound.dao;

import com.jincheng.lostandfound.pojo.Station;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StationMapper {
    List<Station> selectEnabled(@Param("keyword") String keyword);

    Station selectById(@Param("id") Integer id);

    Station selectByName(@Param("stationName") String stationName);

    int insert(Station station);
}
