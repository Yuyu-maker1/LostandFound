package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.pojo.Station;

import java.util.List;

public interface StationService {
    List<Station> listEnabled(String keyword);

    /** 全部启用车站（公开只读接口用） */
    List<Station> listAllEnabled();

    Station getEnabledById(Integer id);

    Station getByName(String stationName);

    boolean addStation(String stationName, String city);
}
