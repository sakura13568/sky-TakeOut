package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
            List<LocalDate> dataList = new ArrayList<>();

            dataList.add(begin);
            while(!begin.equals(end)){
                begin = begin.plusDays(1);
                dataList.add(begin);
            }
         List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dataList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String,Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnOver = orderMapper.sumByMap(map);
            turnOver = turnOver == null ? 0.0 : turnOver;
            turnoverList.add(turnOver);
        }
        String datas = StringUtils.join(dataList, ',');
        String turnOvers = StringUtils.join(turnoverList, ',');
        return TurnoverReportVO.builder()
                 .dateList(datas)
                .turnoverList(turnOvers)
                 .build();
    }
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dataList = new ArrayList<>();

        dataList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dataList.add(begin);
        }
        List<Integer> newUserList = new ArrayList<>();

        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dataList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String,Object> map = new HashMap<>();
            map.put("endTime", endTime);
            Integer newUser = userMapper.countByMap(map);

            map.put("beginTime", beginTime);
            Integer totalUser = userMapper.countByMap(map);
            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dataList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dataList = new ArrayList<>();
        dataList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dataList.add(begin);
        }
        List<Integer> valiadOrderList = new ArrayList<>();
        List<Integer> totalOrderList = new ArrayList<>();
        Integer valiadOrderCount = 0;
        Integer totalOrderCount = 0;
        for (LocalDate date : dataList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String,Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            Integer totalOrder = orderMapper.countByMap(map);
            totalOrder  = totalOrder == null ? 0 : totalOrder;
            totalOrderCount += totalOrder;
            totalOrderList.add(totalOrder);

            map.put("status", Orders.COMPLETED);
            Integer valiadOrder = orderMapper.countByMap(map);
            valiadOrder = valiadOrder == null ? 0 : valiadOrder;
            valiadOrderCount += valiadOrder;
            valiadOrderList.add(valiadOrder);
        }
        Double orderCompletion = 0.0;
        if(totalOrderCount != 0){
            orderCompletion = valiadOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dataList,","))
                .validOrderCount(valiadOrderCount)
                .validOrderCountList(StringUtils.join(valiadOrderList,","))
                .totalOrderCount(totalOrderCount)
                .orderCountList(StringUtils.join(totalOrderList,","))
                .build();
    }
    @Override
    public SalesTop10ReportVO getTop10Statistics(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }
}
