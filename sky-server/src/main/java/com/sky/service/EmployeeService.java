package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

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

    /**
     * 分页查询使用了pageHelper技术简化了分页插叙SQL语句编写
     *
     * @param employeePageQueryDTO
     * @return
     */
     PageResult queryPage(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用或禁用员工
     *
     * @param status
     * @param id
     */
    void startOrstop(Integer status, Long id);


    Employee selectEmpById(Long id);

    void updateEmp(Employee employee);
}
