package org.jetlinks.community.device.entity;

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

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;

/**
 * @author luo'xing'yue
 * @CreateTime 2022-08-12
 */
@Getter
@Setter
@Table(name = "region")
public class DeviceGeoEntity extends GenericEntity<String>{

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE, strategy = GenerationType.IDENTITY)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @Schema(description = "区域ID(只能由数字,字母,下划线和中划线组成)")
    public String getId() {
        return super.getId();
    }


    @Comment("JSON类型")
    @Column(name = "type")
    @NotBlank(message = "JSON类型不能为空", groups = CreateGroup.class)
    @Schema(description = "JSON类型")
    private String type;

    @Comment("区域间关系")
    @Column(name = "features", length = 10000)
    @Schema(description = "特征")
    private String features;




}
