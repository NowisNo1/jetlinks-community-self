package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.community.device.service.DeviceConfigMetadataManager;
import org.jetlinks.community.device.service.DeviceLocationService;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.io.excel.ImportExportService;
import org.jetlinks.core.device.*;
import org.jetlinks.core.metadata.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author luo'xing'yue
 * @CreateTime 2022-08-13
 */
@RestController
@RequestMapping("/device/location")
@Authorize
@Resource(id = "device-location", name = "地理位置")
@Slf4j
@Tag(name = "地理位置")
public class DeviceLocationController implements
    ReactiveServiceCrudController<DeviceInstanceEntity, String> {

    @Getter
    private final LocalDeviceInstanceService service;

    private final DeviceRegistry registry;

    private final LocalDeviceProductService productService;

    private final ImportExportService importExportService;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;
    private final ReactiveRepository<DeviceGeoEntity, String> geoRepository;
    private final DeviceDataService deviceDataService;

    private final DeviceLocationService deviceLocationService;

    private final DeviceConfigMetadataManager metadataManager;


    public DeviceLocationController(LocalDeviceInstanceService service,
                                    DeviceRegistry registry,
                                    LocalDeviceProductService productService,
                                    ImportExportService importExportService,
                                    ReactiveRepository<DeviceTagEntity, String> tagRepository,
                                    ReactiveRepository<DeviceGeoEntity, String> geoRepository, DeviceDataService deviceDataService,
                                    DeviceLocationService deviceLocationService, DeviceConfigMetadataManager metadataManager) {

        this.service = service;
        this.registry = registry;

        this.productService = productService;
        this.importExportService = importExportService;
        this.tagRepository = tagRepository;
        this.geoRepository = geoRepository;
        this.deviceDataService = deviceDataService;
        this.deviceLocationService = deviceLocationService;
        this.metadataManager = metadataManager;
    }


    //获取设备详情
    @GetMapping("/{id:.+}/detail")
    @QueryAction
    @Operation(summary = "获取指定ID设备详情")
    public Mono<DeviceDetail> getDeviceDetailInfo(@PathVariable @Parameter(description = "设备ID") String id) {
        return service.getDeviceDetail(id);
    }

    //获取设备详情
    @GetMapping("/{id:.+}/config-metadata")
    @QueryAction
    @Operation(summary = "获取设备需要的配置定义信息")
    public Flux<ConfigMetadata> getDeviceConfigMetadata(@PathVariable @Parameter(description = "设备ID") String id) {
        return metadataManager.getDeviceConfigMetadata(id);
    }


    @PostMapping("/geo/object/_search/geo.json")
    @QueryAction
    @Operation(summary = "获取全部区域")
    public Flux<DeviceGeoEntity> getAllRegionData() {
        System.out.println("获取区域信息");
        return geoRepository
           .createQuery()
           .fetch();
    }
    @PostMapping("/geo/object/geo.json")
    @QueryAction
    @Operation(summary = "保存区域信息")
    public Flux<DeviceGeoEntity> saveDeviceTag(@RequestBody Flux<DeviceGeoEntity> geoes) {
        return geoes
            .doOnNext(geo -> {
                geo.setFeatures(geo.getFeatures());
                geo.setType(geo.getType());
                geo.tryValidate();
            })
            .as(geoRepository::save)
            .thenMany(getAllRegionData());
    }

    /**
     * 获取设备全部标签
     * <pre>
     *     GET /device/instance/{deviceId}/tags
     *
     *     [
     *      {
     *          "id":"id",
     *          "key":"",
     *          "value":"",
     *          "name":""
     *      }
     *     ]
     * </pre>
     *
     *
     * @return 设备标签列表
     */
    @GetMapping("/tags")
    @SaveAction
    @Operation(summary = "获取设备全部标签数据")
    public Flux<DeviceTagEntity> getDeviceTags() {
        return tagRepository.createQuery()
            .fetch();
    }


}
