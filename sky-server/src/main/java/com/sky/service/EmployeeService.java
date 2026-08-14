package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO 员工登录信息
     * @return 登录成功返回
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工。
     * 密码使用 MD5 加密（默认密码），状态默认为启用；
     * 创建人使用ThreadLocal后去操作用户id
     *
     * @param employeeDTO 员工基本信息
     */
    void save(EmployeeDTO employeeDTO);
}
