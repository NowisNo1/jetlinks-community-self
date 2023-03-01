package org.jetlinks.community.device.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceGeoInfo {

    private String type;
    private String features;

}
