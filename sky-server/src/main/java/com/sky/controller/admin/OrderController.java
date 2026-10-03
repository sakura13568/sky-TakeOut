package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@Slf4j
@Api(tags = "商家订单相关接口")
@RequestMapping("/admin/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> pageSearchOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单搜索,{}",ordersPageQueryDTO);
        PageResult pageResult = orderService.pageSearchOrders(ordersPageQueryDTO);
        return Result.success(pageResult);
    }
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> showOrderDetail(@PathVariable("id")  Long id) {
        log.info("要查询订单的id:{}",id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }
    @GetMapping("/statistics")
    @ApiOperation("统计各个状态订单数量")
    public Result<OrderStatisticsVO> countOrderNumber(){
        log.info("统计各个状态订单数量");
        OrderStatisticsVO orderStatisticsVO = orderService.countOrderNumber();
        return Result.success(orderStatisticsVO);
    }
    @PutMapping("/confirm")
    @ApiOperation("商家接单")
    public Result confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
         log.info("要接收订单的id:{}",ordersConfirmDTO.getId());
         orderService.confirmOrder(ordersConfirmDTO);
         return Result.success();
    }
    @PutMapping("/rejection")
    @ApiOperation("商家拒绝订单操作")
    public Result rejectOrder(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        log.info("商家拒绝接单的单号:{}",ordersRejectionDTO.getId());
        orderService.rejectOrders(ordersRejectionDTO);
        return Result.success();
    }
    @PutMapping("/cancel")
    @ApiOperation("商家取消订单")
    public Result cancelOrders(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        log.info("商家取消订单号:{}",ordersCancelDTO.getId());
        orderService.adminCancelOrder(ordersCancelDTO);
        return Result.success();
    }
    @PutMapping("/delivery/{id}")
    @ApiOperation("商家派送订单")
    public Result deliveryOrders(@PathVariable Long id){
        log.info("商家派送订单号:{]",id);
        orderService.deliveryOrders(id);
        return Result.success();
    }
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }
}
