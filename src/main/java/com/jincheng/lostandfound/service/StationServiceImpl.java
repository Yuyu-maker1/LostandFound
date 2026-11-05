package com.jincheng.lostandfound.service;

import com.jincheng.lostandfound.dao.StationMapper;
import com.jincheng.lostandfound.pojo.Station;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class StationServiceImpl implements StationService {
    @Resource
    private StationMapper stationMapper;

    @Override
    public List<Station> listEnabled(String keyword) {
        return stationMapper.selectEnabled(keyword);
    }

    @Override
    public Station getEnabledById(Integer id) {
        if (id == null) {
            return null;
        }
        Station station = stationMapper.selectById(id);
        if (station == null || station.getIsEnabled() == null || station.getIsEnabled() != 1) {
            return null;
        }
        return station;
    }

    @Override
    public Station getByName(String stationName) {
        return stationMapper.selectByName(stationName);
    }

    @Override
    public boolean addStation(String stationName, String city) {
        Station station = new Station();
        station.setStationName(stationName);
        station.setCity(city);
        return stationMapper.insert(station) > 0;
    }
}
