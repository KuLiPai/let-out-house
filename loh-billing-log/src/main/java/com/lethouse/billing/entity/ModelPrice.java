package com.lethouse.billing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("model_price")
public class ModelPrice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelName;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private String currency;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
