package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.WeChatProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.beancontext.BeanContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        checkOutOfDistance(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.getShoppingCartByUserId(userId);
        if(shoppingCarts == null || shoppingCarts.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setUserId(userId);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setOrderTime(LocalDateTime.now());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        orderMapper.insert(orders);
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart shoppingCart : shoppingCarts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        shoppingCartMapper.deleteShoppingCartsByUserId(userId);

        OrderSubmitVO submitVO = OrderSubmitVO.builder()
                .orderTime(orders.getOrderTime())
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return submitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */

    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 模拟支付：无商户号环境下，点击支付即视为支付成功，直接推进订单状态
        if (weChatProperties.isMockPay()) {
            paySuccess(ordersPaymentDTO.getOrderNumber());
            // 返回空 VO，前端约定 packageStr 为空 => 视为支付成功，跳过 wx.requestPayment
            return new OrderPaymentVO();
        }

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        HashMap<String, Object> map = new HashMap<>();
        map.put("type","1"); // 1表示来单提醒，2表示客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号:" + outTradeNo);
        String message = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(message);
    }
    @Override
    public PageResult showHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO)  {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Integer status = ordersPageQueryDTO.getStatus();
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> page = orderMapper.pageSearchHistoryOrders(ordersPageQueryDTO);
        List<Orders> orders = page.getResult();
        List<OrderVO> orderVOS = new ArrayList<>();
        for (Orders order : orders) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order,orderVO);
            List<OrderDetail> list = orderDetailMapper.getAllByOrderId(order.getId());
            orderVO.setOrderDetailList(list);
            orderVOS.add(orderVO);
        }
        return new PageResult(page.getTotal(),orderVOS);
    }
    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper. getAllByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }
    @Override
    @Transactional
    public void cancelOrder(Long id)  {
        Orders ordersDB = orderMapper.getById(id);
        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if(ordersDB.getPayStatus().equals(Orders.PAID)){
            try {
                refundIfNeeded(ordersDB);
                ordersDB.setPayStatus(Orders.REFUND);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
       ordersDB.setStatus(Orders.CANCELLED);
        ordersDB.setCancelTime(LocalDateTime.now());
        ordersDB.setCancelReason("用户取消");
        orderMapper.update(ordersDB);
    }
    @Override
    public void orderAgain(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        Long userId = ordersDB.getUserId();
        List<OrderDetail> orderDetails = orderDetailMapper.getAllByOrderId(ordersDB.getId());
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCarts.add(shoppingCart);
        }
        shoppingCartMapper.insertShoppingCartsBatch(shoppingCarts);
    }
    @Override
    public PageResult pageSearchOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageSearchHistoryOrders(ordersPageQueryDTO);
        List<Orders> orders = page.getResult();
        List<OrderVO> orderVOList = getOrderVOList(orders);
        return new PageResult(page.getTotal(),orderVOList);
    }
    private List<OrderVO> getOrderVOList(List<Orders> orders) {
        List<OrderVO> orderVOS = new ArrayList<>();
        for (Orders order : orders) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order,orderVO);
            Long orderId = order.getId();
            String orderDishes = getOrderDishes(orderId);
            orderVO.setOrderDishes(orderDishes);
            orderVOS.add(orderVO);
        }
        return orderVOS;
    }
   private String getOrderDishes(Long orderId) {
            List<OrderDetail> orderDetails = orderDetailMapper.getAllByOrderId(orderId);
            StringBuilder orderDishes = new StringBuilder();
            for (OrderDetail orderDetail : orderDetails) {
                 orderDishes.append(orderDetail.getName()).append("*").append(orderDetail.getNumber()).append(";");
            }
       return orderDishes.toString();
   }
   @Override
    public OrderStatisticsVO countOrderNumber(){
        List<Orders> orders = orderMapper.getAllOrders();
        Integer toBeConfirmed = 0;
        Integer confirmed = 0;
        Integer delivered = 0;
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        if(orders == null || orders.size() == 0){
            orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
            orderStatisticsVO.setConfirmed(confirmed);
            orderStatisticsVO.setDeliveryInProgress(delivered);
            return orderStatisticsVO;
        }
        for(Orders order : orders){
            if(Objects.equals(order.getStatus(), Orders.TO_BE_CONFIRMED)){
                toBeConfirmed++;
            }else if(Objects.equals(order.getStatus(), Orders.CONFIRMED)){
                confirmed++;
            }else if(Objects.equals(order.getStatus(), Orders.DELIVERY_IN_PROGRESS)){
                delivered++;
            }
        }
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(delivered);
        return orderStatisticsVO;
   }
   @Override
    public void confirmOrder(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();
        ordersConfirmDTO.setStatus(Orders.CONFIRMED);
        BeanUtils.copyProperties(ordersConfirmDTO,orders);

        orderMapper.update(orders);
   }
   @Override
    public void rejectOrders(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
       // 根据id查询订单
       Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

       // 订单只有存在且状态为2（待接单）才可以拒单
       if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
           throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
       }

       //支付状态：已支付需退款（模拟支付下跳过真实微信退款）
       if (ordersDB.getPayStatus().equals(Orders.PAID)) {
           refundIfNeeded(ordersDB);
       }

       // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
       Orders orders = new Orders();
       orders.setId(ordersDB.getId());
       orders.setStatus(Orders.CANCELLED);
       orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
       orders.setCancelTime(LocalDateTime.now());

       orderMapper.update(orders);
   }
   @Override
    public void adminCancelOrder(OrdersCancelDTO ordersCancelDTO) throws Exception {
       Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
       if (ordersDB == null){
           throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
       }
       if(ordersDB.getStatus() == Orders.TO_BE_CONFIRMED){
           throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
       }
       //支付状态：已支付需退款（模拟支付下跳过真实微信退款）
       if (ordersDB.getPayStatus().equals(Orders.PAID)) {
           refundIfNeeded(ordersDB);
       }

       // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
       Orders orders = new Orders();
       orders.setId(ordersCancelDTO.getId());
       orders.setStatus(Orders.CANCELLED);
       orders.setCancelReason(ordersCancelDTO.getCancelReason());
       orders.setCancelTime(LocalDateTime.now());
       orderMapper.update(orders);
   }
   @Override
    public void deliveryOrders(Long id ){
       Orders ordersDB = orderMapper.getById(id);

       // 校验订单是否存在，并且状态为3
       if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
           throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
       }

       Orders orders = new Orders();
       orders.setId(ordersDB.getId());
       // 更新订单状态,状态转为派送中
       orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

       orderMapper.update(orders);
   }
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }
    /**
     * 退款处理：真实支付且已支付时调用微信退款；模拟支付下为 no-op
     * @param ordersDB 订单
     */
    private void refundIfNeeded(Orders ordersDB) throws Exception {
        if (!weChatProperties.isMockPay() && ordersDB.getPayStatus().equals(Orders.PAID)) {
            weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
        }
    }

    @Override
    public void checkOutOfDistance(String address) {
        // 1. 地址 -> 坐标（格式 "lat,lng"）
        String shopCoordinate = getCoordinate(shopAddress);
        String userCoordinate = getCoordinate(address);

        // 2. 驾车路线规划，取距离（单位：米）
        HashMap<String, String> params = new HashMap<>();
        params.put("origin", shopCoordinate);
        params.put("destination", userCoordinate);
        params.put("ak", ak);

        String resp = HttpClientUtil.doGet("https://api.map.baidu.com/direction/v2/driving", params);
        JSONObject json = JSONObject.parseObject(resp);
        if (json.getInteger("status") != 0) {
            throw new OrderBusinessException("路线规划失败: " + json.getString("message"));
        }

        JSONArray routes = json.getJSONObject("result").getJSONArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new OrderBusinessException("未获取到配送路线");
        }

        int distance = routes.getJSONObject(0).getInteger("distance");
        if (distance > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /** 地址转坐标，返回 "lat,lng" */
    private String getCoordinate(String address) {
        HashMap<String, String> params = new HashMap<>();
        params.put("address", address);
        params.put("output", "json");
        params.put("ak", ak);

        String resp = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3/", params);
        JSONObject json = JSONObject.parseObject(resp);
        if (json.getInteger("status") != 0) {
            throw new OrderBusinessException("地址解析失败: " + json.getString("message"));
        }

        JSONObject location = json.getJSONObject("result").getJSONObject("location");
        float lat = location.getFloat("lat");
        float lng = location.getFloat("lng");
        return lat + "," + lng;
    }
    @Override
    public void reminder(Long id){
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map<String,Object> params = new HashMap<>();
        params.put("orderId",ordersDB.getId());
        params.put("type","2");
        params.put("content","订单号:" + ordersDB.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(params));
    }
}
