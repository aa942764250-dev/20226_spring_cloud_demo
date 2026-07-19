package com.example.service.encrypt;

import com.example.common.encrypt.EncryptAlgorithm;
import com.example.common.encrypt.EncryptErrorCode;
import com.example.common.encrypt.EncryptException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 加密算法路由器。
 * <p>
 * 维护算法标识到 {@link EncryptAlgorithm} 实现的映射关系，
 * 支持根据算法标识路由到具体实现，以及通过密文前缀判断数据是否已被加密。
 * 算法实现通过Spring自动注入，运行时按需注册。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class EncryptAlgorithmRouter {

    /** Spring自动注入的所有加密算法实现 */
    private final List<EncryptAlgorithm> algorithms;

    /** 算法标识到实现的映射（懒加载） */
    private Map<String, EncryptAlgorithm> algorithmMap;

    /** 密文中算法标识前缀的分隔符，默认为 ":" */
    private String prefixSeparator = ":";

    /**
     * 获取算法标识到实现的映射，懒加载初始化。
     *
     * @return 算法标识到实现的映射Map
     */
    private Map<String, EncryptAlgorithm> getAlgorithmMap() {
        if (algorithmMap == null) {
            algorithmMap = algorithms.stream()
                    .collect(Collectors.toMap(EncryptAlgorithm::getAlgorithmId, Function.identity()));
        }
        return algorithmMap;
    }

    /**
     * 根据算法标识路由到对应的加密算法实现。
     *
     * @param algorithmId 算法标识，如 "SM4"、"AES"
     * @return 对应的加密算法实现
     * @throws EncryptException 算法标识不存在时抛出 ENCRYPT_ALGORITHM_NOT_FOUND
     */
    public EncryptAlgorithm route(String algorithmId) {
        EncryptAlgorithm algorithm = getAlgorithmMap().get(algorithmId);
        if (algorithm == null) {
            throw new EncryptException(EncryptErrorCode.ENCRYPT_ALGORITHM_NOT_FOUND, algorithmId);
        }
        return algorithm;
    }

    /**
     * 判断值是否已被加密。
     * <p>
     * 通过检查值是否包含已注册算法标识的前缀来判断，如 "SM4:xxx" 会被识别为已加密。
     * </p>
     *
     * @param value 待判断的值
     * @return true=已加密，false=未加密或值为空
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String prefix = parseAlgorithmPrefix(value);
        return prefix != null && getAlgorithmMap().containsKey(prefix);
    }

    /**
     * 解析密文中的算法标识前缀。
     * <p>
     * 从密文字符串中提取分隔符前的算法标识，仅当该标识已注册时才返回。
     * </p>
     *
     * @param value 密文值
     * @return 算法标识，无前缀或标识未注册时返回 null
     */
    public String parseAlgorithmPrefix(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        int idx = value.indexOf(prefixSeparator);
        if (idx <= 0) {
            return null;
        }
        String prefix = value.substring(0, idx);
        return getAlgorithmMap().containsKey(prefix) ? prefix : null;
    }

    /**
     * 设置密文中算法标识前缀的分隔符。
     *
     * @param separator 分隔符，默认为 ":"
     */
    public void setPrefixSeparator(String separator) {
        this.prefixSeparator = separator;
    }
}
