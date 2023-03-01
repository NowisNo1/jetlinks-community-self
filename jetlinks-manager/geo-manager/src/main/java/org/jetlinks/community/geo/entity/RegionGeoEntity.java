package org.jetlinks.community.geo.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;


import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;


@Getter
@Setter
@Table(name = "region")
public class RegionGeoEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(
        regexp = "^[0-9a-zA-Z_\\-]+$",
        message = "ID只能由数字,字母,下划线和中划线组成",
        groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId() {
        return super.getId();
    }

    @Comment("JSON类型")
    @Column(name = "type")
    @NotBlank(message = "JSON类型不能为空", groups = CreateGroup.class)
    @Schema(description = "JSON类型")
    private String type;

    @Comment("区域间关系")
    @Column(name = "features")
    @Schema(description = "特征")
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String features;

    @Comment("地理点集")
    @Column(name = "geometry")
    @Schema(description = "点集")
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String geometry;

    @Column(name = "creator_id")
    @Comment("创建者id")
    @Schema(description = "创建者ID(只读)")
    private String creatorId;

    @Comment("创建时间")
    @Column(name = "create_time")
    @Schema(description = "创建者时间(只读)")
    private Long createTime;


}
