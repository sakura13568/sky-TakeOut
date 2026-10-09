package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;



@RestController
@Slf4j
@RequestMapping("/admin/report")
@Api(tags = "报表相关接口")
public class ReportController {
    @Autowired
    private ReportService reportService;
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("统计{}-{}时间段内的营业额数据", begin, end);
        TurnoverReportVO turnoverStatistics = reportService.getTurnoverStatistics(begin, end);
        return Result.success(turnoverStatistics);
    }
    @GetMapping("/userStatistics")
    @ApiOperation("用户数量统计")
    public Result<UserReportVO> userStatistic(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end){
        log.info("统计{}-{}时间段内的用户数据", begin, end);
        UserReportVO userReport = reportService.getUserStatistics(begin,end);
        return Result.success(userReport);
    }
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单数据统计")
    public Result<OrderReportVO> orderStatistics(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end){
        log.info("统计{}-{}时间段内的订单数据", begin, end);
        OrderReportVO reportVO =  reportService.getOrderStatistics(begin,end);
        return Result.success(reportVO);
    }
   @GetMapping("/top10")
   public Result<SalesTop10ReportVO> top10Statistics(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end){
       log.info("统计{}-{}时间段内销量top10的菜品或套餐", begin, end);
       return Result.success(reportService.getTop10Statistics(begin,end));
   }
   @GetMapping("/export")
   @ApiOperation("导出运营数据报表")
    public void export(HttpServletResponse response,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate begin,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate end){
        log.info("导出{}-{}时间段内的运营数据报表", begin, end);
        reportService.export(response, begin, end);
   }
}
