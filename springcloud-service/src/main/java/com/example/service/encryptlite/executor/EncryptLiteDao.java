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

    List<Map<String, Object>> batchSelect(@Param("tableName") String tableName,
                                          @Param("columnNames") List<String> columnNames,
                                          @Param("pkColumnName") String pkColumnName,
                                          @Param("lastPkValue") Object lastPkValue,
                                          @Param("batchSize") int batchSize);

    int updateRecord(@Param("tableName") String tableName,
                     @Param("fieldValues") Map<String, String> fieldValues,
                     @Param("pkColumnName") String pkColumnName,
                     @Param("pkValue") Object pkValue);

    String queryPrimaryKeyColumn(@Param("tableName") String tableName);

    boolean checkTableExists(@Param("tableName") String tableName);

    boolean checkFieldExists(@Param("tableName") String tableName,
                             @Param("fieldName") String fieldName);

    /**
     * 统计待加密行数（非空且非V1已加密）。
     *
     * @param tableName 目标表名
     * @param fields    加密字段列表
     * @return 至少有一个字段待加密的行数
     */
    long countCandidates(@Param("tableName") String tableName,
                         @Param("fields") List<String> fields);

    /**
     * 查询字段字符长度（用于校验密文长度是否兼容）。
     * 返回 0 表示 CLOB/TEXT 等不限长类型。
     *
     * @param tableName 目标表名
     * @param fieldName 字段名
     * @return 字符长度，0 表示不限长
     */
    long queryFieldCharLength(@Param("tableName") String tableName,
                              @Param("fieldName") String fieldName);
}
