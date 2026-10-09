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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
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
    @Override
    public void export(HttpServletResponse response, LocalDate begin, LocalDate end) {
        // 未传时间时，默认导出最近 30 天
        if (begin == null) {
            begin = LocalDate.now().minusDays(29);
        }
        if (end == null) {
            end = LocalDate.now();
        }

        // 1. 构建日期列表
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cursor = begin;
        while (!cursor.isAfter(end)) {
            dateList.add(cursor);
            cursor = cursor.plusDays(1);
        }

        // 2. 逐日统计，同时累计总量
        double totalTurnover = 0.0;
        int totalValidOrders = 0;
        int totalOrders = 0;
        int totalNewUsers = 0;

        List<Double> turnoverList = new ArrayList<>();
        List<Integer> validOrderList = new ArrayList<>();
        List<Double> completionRateList = new ArrayList<>();
        List<Double> avgPriceList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("beginTime", beginTime);
            orderMap.put("endTime", endTime);
            orderMap.put("status", Orders.COMPLETED);

            Double turnoverVal = orderMapper.sumByMap(orderMap);
            double turnover = turnoverVal == null ? 0.0 : turnoverVal;

            Integer validVal = orderMapper.countByMap(orderMap);
            int validOrders = validVal == null ? 0 : validVal;

            orderMap.remove("status");
            Integer totalVal = orderMapper.countByMap(orderMap);
            int totalOrdersOfDay = totalVal == null ? 0 : totalVal;

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("beginTime", beginTime);
            userMap.put("endTime", endTime);
            Integer newUserVal = userMapper.countByMap(userMap);
            int newUsers = newUserVal == null ? 0 : newUserVal;

            double completionRate = totalOrdersOfDay == 0 ? 0.0 : validOrders * 1.0 / totalOrdersOfDay;
            double avgPrice = validOrders == 0 ? 0.0 : turnover / validOrders;

            turnoverList.add(turnover);
            validOrderList.add(validOrders);
            completionRateList.add(completionRate);
            avgPriceList.add(avgPrice);
            newUserList.add(newUsers);

            totalTurnover += turnover;
            totalValidOrders += validOrders;
            totalOrders += totalOrdersOfDay;
            totalNewUsers += newUsers;
        }

        // 3. 读取模板、填充数据并写出到响应流
        try (InputStream in = new ClassPathResource("template/report-template.xlsx").getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);

            // 概览数据
            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            sheet.getRow(3).getCell(2).setCellValue(totalTurnover);
            sheet.getRow(3).getCell(4).setCellValue(totalOrders == 0 ? 0.0 : totalValidOrders * 1.0 / totalOrders);
            sheet.getRow(3).getCell(6).setCellValue(totalNewUsers);
            sheet.getRow(4).getCell(2).setCellValue(totalValidOrders);
            sheet.getRow(4).getCell(4).setCellValue(totalValidOrders == 0 ? 0.0 : totalTurnover / totalValidOrders);

            // 明细数据（模板只有 30 行）
            int rowCount = Math.min(dateList.size(), 30);
            for (int i = 0; i < rowCount; i++) {
                Row row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(dateList.get(i).toString());
                row.getCell(2).setCellValue(turnoverList.get(i));
                row.getCell(3).setCellValue(validOrderList.get(i));
                row.getCell(4).setCellValue(completionRateList.get(i));
                row.getCell(5).setCellValue(avgPriceList.get(i));
                row.getCell(6).setCellValue(newUserList.get(i));
            }

            // 设置响应头并写出
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("运营数据报表.xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException("导出运营数据报表失败", e);
        }
    }
}
