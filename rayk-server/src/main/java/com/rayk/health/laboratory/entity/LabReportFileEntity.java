package com.rayk.health.laboratory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_report_file")
public class LabReportFileEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long reportId;
    private String bucketName;
    private String objectPath;
    private String originalName;
    private String mimeType;
    private Long fileSize;
    private String sha256;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer version;
}

