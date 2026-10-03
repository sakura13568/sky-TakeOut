package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    void insertBatch(List<OrderDetail> orderDetails);
    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getAllByOrderId(Long id);

    @Delete("delete from order_detail where order_id = #{id}")
    void deleteOrderDetails(Long id);
}
