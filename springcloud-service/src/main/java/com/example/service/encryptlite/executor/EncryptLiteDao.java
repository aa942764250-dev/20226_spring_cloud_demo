package com.example.service.encryptlite.executor;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 轻量加密组件动态表操作DAO。
 * <p>
 * 提供对任意业务表的动态查询和更新能力，SQL实现见 EncryptLiteMapper.xml。
 * </p>
 */
@Mapper
public interface EncryptLiteDao {

    /**
     * 按主键排序分批读取指定表的指定字段数据。
     *
     * @param tableName    目标表名
     * @param columnNames  需查询的列名列表（含主键列）
     * @param pkColumnName 主键列名
     * @param lastPkValue  上一批次最后一条记录的主键值（首次传null）
     * @param batchSize    批次大小
     * @return 查询结果列表
     */
    List<Map<String, Object>> batchSelect(@Param("tableName") String tableName,
                                          @Param("columnNames") List<String> columnNames,
                                          @Param("pkColumnName") String pkColumnName,
                                          @Param("lastPkValue") Object lastPkValue,
                                          @Param("batchSize") int batchSize);

    /**
     * 更新指定表中单条记录的多个字段值。
     *
     * @param tableName    目标表名
     * @param fieldValues  字段名到新值的映射
     * @param pkColumnName 主键列名
     * @param pkValue      主键值
     * @return 受影响的行数
     */
    int updateRecord(@Param("tableName") String tableName,
                     @Param("fieldValues") Map<String, String> fieldValues,
                     @Param("pkColumnName") String pkColumnName,
                     @Param("pkValue") Object pkValue);

    /**
     * 查询指定表的主键列名。
     *
     * @param tableName 目标表名
     * @return 主键列名
     */
    String queryPrimaryKeyColumn(@Param("tableName") String tableName);

    /**
     * 检查指定表是否存在。
     *
     * @param tableName 目标表名
     * @return true=存在
     */
    boolean checkTableExists(@Param("tableName") String tableName);

    /**
     * 检查指定字段是否存在于目标表中。
     *
     * @param tableName 目标表名
     * @param fieldName 字段名
     * @return true=存在
     */
    boolean checkFieldExists(@Param("tableName") String tableName,
                             @Param("fieldName") String fieldName);
}