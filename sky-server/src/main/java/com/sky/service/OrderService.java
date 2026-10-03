package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult showHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查看订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 根据订单号取消该所有有关该订单的信息
     * @param id 要取消的订单号
     */
    void cancelOrder(Long id);

    void orderAgain(Long id);

    /**
     * 通过前端发送的查询条件查询服务端所有订单信息
     * @param ordersPageQueryDTO 查询参数
     * @return
     */
    PageResult pageSearchOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计各个状态订单的数量
     * @return
     */
    OrderStatisticsVO countOrderNumber();

    void confirmOrder(OrdersConfirmDTO ordersConfirmDTO);

    void rejectOrders(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */

    void adminCancelOrder(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 商家派送订单
     * @param id
     */
    void deliveryOrders(Long id);

    void complete(Long id);

    void checkOutOfDistance(String address);

    void reminder(Long id);
}
