package com.DocSystem.agent.learning.repository;

import com.DocSystem.agent.learning.entity.UserCustomLlmModel;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * User Custom LLM Model Repository - 用户自定义 LLM 模型仓储
 * MyBatis mapper interface.
 *
 * 写操作(update/delete)一律带 user_id 条件，杜绝越权修改他人模型。
 */
public interface UserCustomLlmModelRepository {

    List<UserCustomLlmModel> findByUserId(@Param("userId") String userId);

    UserCustomLlmModel findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    int insert(UserCustomLlmModel model);

    int updateByIdAndUserId(UserCustomLlmModel model);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);
}
