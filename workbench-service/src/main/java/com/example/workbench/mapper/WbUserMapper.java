package com.example.workbench.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.workbench.entity.WbUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WbUserMapper extends BaseMapper<WbUser> {

    @Select("SELECT r.role_code FROM wb_role r " +
            "INNER JOIN wb_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.* FROM wb_menu m " +
            "INNER JOIN wb_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN wb_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.menu_type IN (1,2) " +
            "ORDER BY m.sort_order ASC")
    List<com.example.workbench.entity.WbMenu> selectMenusByUserId(@Param("userId") Long userId);
}
