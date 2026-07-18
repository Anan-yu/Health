package com.rayk.health.patient.converter;

import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.vo.PatientVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientConverter {
    @Mapping(target = "id", expression = "java(String.valueOf(entity.getId()))")
    @Mapping(
            target = "assignedDoctorId",
            expression =
                    "java(entity.getAssignedDoctorId() == null ? null : String.valueOf(entity.getAssignedDoctorId()))")
    @Mapping(
            target = "assignedManagerId",
            expression =
                    "java(entity.getAssignedManagerId() == null ? null : String.valueOf(entity.getAssignedManagerId()))")
    PatientVo toVo(PatientEntity entity);
}

