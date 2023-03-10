package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.reactor.excel.ReactorExcel;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryNoPagingOperation;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.community.device.response.ImportDeviceInstanceResult;
import org.jetlinks.community.device.service.DeviceConfigMetadataManager;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.web.excel.DeviceExcelInfo;
import org.jetlinks.community.device.web.excel.DeviceWrapper;
import org.jetlinks.community.device.web.request.AggRequest;
import org.jetlinks.community.io.excel.ImportExportService;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.manager.DeviceBindHolder;
import org.jetlinks.core.device.manager.DeviceBindProvider;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.metadata.*;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.util.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hswebframework.reactor.excel.ReactorExcel.*;

@RestController
@RequestMapping({"/device-instance", "/device/instance"})
@Authorize
@Resource(id = "device-instance", name = "????????????")
@Slf4j
@Tag(name = "??????????????????")
public class DeviceInstanceController implements
    ReactiveServiceCrudController<DeviceInstanceEntity, String> {

    @Getter
    private final LocalDeviceInstanceService service;

    private final DeviceRegistry registry;

    private final LocalDeviceProductService productService;

    private final ImportExportService importExportService;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final ReactiveRepository<DeviceInstanceEntity, String> instanceRepository;

    private final DeviceDataService deviceDataService;

    private final DeviceConfigMetadataManager metadataManager;

    @SuppressWarnings("all")
    public DeviceInstanceController(LocalDeviceInstanceService service,
                                    DeviceRegistry registry,
                                    LocalDeviceProductService productService,
                                    ImportExportService importExportService,
                                    ReactiveRepository<DeviceTagEntity, String> tagRepository,
                                    ReactiveRepository<DeviceInstanceEntity, String> instanceRepository, DeviceDataService deviceDataService,
                                    DeviceConfigMetadataManager metadataManager) {

        this.service = service;
        this.registry = registry;
        this.productService = productService;
        this.importExportService = importExportService;
        this.tagRepository = tagRepository;
        this.instanceRepository = instanceRepository;
        this.deviceDataService = deviceDataService;
        this.metadataManager = metadataManager;
    }


    //??????????????????
    @GetMapping("/{id:.+}/detail")
    @QueryAction
    @Operation(summary = "????????????ID????????????")
    public Mono<DeviceDetail> getDeviceDetailInfo(@PathVariable @Parameter(description = "??????ID") String id) {
        return service.getDeviceDetail(id);
    }

    //??????????????????
    @GetMapping("/{id:.+}/config-metadata")
    @QueryAction
    @Operation(summary = "???????????????????????????????????????")
    public Flux<ConfigMetadata> getDeviceConfigMetadata(@PathVariable @Parameter(description = "??????ID") String id) {

        return metadataManager.getDeviceConfigMetadata(id);
    }


    @GetMapping("/{id:.+}/config-metadata/{metadataType}/{metadataId}/{typeId}")
    @QueryAction
    @Operation(summary = "??????????????????????????????????????????")
    public Flux<ConfigMetadata> getExpandsConfigMetadata(@PathVariable @Parameter(description = "??????ID") String id,
                                                         @PathVariable @Parameter(description = "???????????????") DeviceMetadataType metadataType,
                                                         @PathVariable @Parameter(description = "?????????ID") String metadataId,
                                                         @PathVariable @Parameter(description = "??????ID") String typeId) {
        return service
            .findById(id)
            .flatMapMany(device -> metadataManager
                .getMetadataExpandsConfig(device.getProductId(), metadataType, metadataId, typeId, DeviceConfigScope.device));
    }

    /**
     * @author luoxingyue
     * @createTime 2022-08-16
     * @param id ??????Id
     * @return
     */
    @PostMapping("/list/{id:.+}")
    @QueryAction
    @Operation(summary = "????????????Id????????????")
    public Flux<DeviceInstanceEntity> getInstanceByProductId(@PathVariable @Parameter(description = "??????ID") String id) {
        return instanceRepository
            .createQuery()
            .where(DeviceInstanceEntity::getProductId, id)
            .fetch();
    }

    /**
     * @author luoxingyue
     * @createTime 2022-08-16
     * @param name ????????????
     * @return
     */
    @PostMapping("/device_name/{name}")
    @QueryAction
    @Operation(summary = "??????????????????????????????")
    public Flux<DeviceInstanceEntity> getInstanceByName(@PathVariable @Parameter(description = "????????????") String name) {
        return instanceRepository
            .createQuery()
            .where(DeviceInstanceEntity::getName, name)
            .fetch();
    }

    @GetMapping("/bind-providers")
    @QueryAction
    @Operation(summary = "???????????????????????????")
    public Flux<DeviceBindProvider> getBindProviders() {
        return Flux.fromIterable(DeviceBindHolder.getAllProvider());
    }


    //????????????????????????
    @GetMapping("/{id:.+}/state")
    @QueryAction
    @Operation(summary = "????????????ID??????????????????")
    public Mono<DeviceState> getDeviceState(@PathVariable @Parameter(description = "??????ID") String id) {
        return service.getDeviceState(id);
    }

    //????????????
    @PostMapping("/{deviceId:.+}/deploy")
    @SaveAction
    @Operation(summary = "????????????ID??????")
    public Mono<DeviceDeployResult> deviceDeploy(@PathVariable @Parameter(description = "??????ID") String deviceId) {
        return service.deploy(deviceId);
    }

    //??????????????????
    @PutMapping("/{deviceId:.+}/configuration/_reset")
    @SaveAction
    @Operation(summary = "????????????????????????")
    public Mono<Map<String, Object>> resetConfiguration(@PathVariable @Parameter(description = "??????ID") String deviceId) {
        return service.resetConfiguration(deviceId);
    }

    //??????????????????
    @GetMapping(value = "/deploy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    @QueryOperation(summary = "???????????????????????????")
    public Flux<DeviceDeployResult> deployAll(@Parameter(hidden = true) QueryParamEntity query) {
        query.setPaging(false);
        return service.query(query).as(service::deploy);
    }

    //????????????
    @PostMapping("/{deviceId:.+}/undeploy")
    @SaveAction
    @Operation(summary = "????????????ID?????????")
    public Mono<Integer> unDeploy(@PathVariable @Parameter(description = "??????ID") String deviceId) {
        return service.unregisterDevice(deviceId);
    }

    //????????????
    @PostMapping("/{deviceId:.+}/disconnect")
    @SaveAction
    @Operation(summary = "????????????ID???????????????")
    public Mono<Boolean> disconnect(@PathVariable @Parameter(description = "??????ID") String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMapMany(DeviceOperator::disconnect)
            .singleOrEmpty();
    }

    //????????????
    @PostMapping
    @Operation(summary = "????????????")
    public Mono<DeviceInstanceEntity> add(@RequestBody Mono<DeviceInstanceEntity> payload) {
        return Mono
            .zip(payload, Authentication.currentReactive(), this::applyAuthentication)
            .flatMap(entity -> service.insert(Mono.just(entity)).thenReturn(entity))
            .onErrorMap(DuplicateKeyException.class, err -> new BusinessException("??????ID?????????", err));
    }

    /**
     * ????????????????????????
     *
     * @param query ????????????
     * @return ??????????????????
     */
    @GetMapping(value = "/state/_sync", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    @QueryNoPagingOperation(summary = "??????????????????")
    public Flux<Integer> syncDeviceState(@Parameter(hidden = true) QueryParamEntity query) {
        query.setPaging(false);
        return service
            .query(query.includes("id"))
            .map(DeviceInstanceEntity::getId)
            .buffer(200)
            .publishOn(Schedulers.single())
            .concatMap(flux -> service.syncStateBatch(Flux.just(flux), true).map(List::size))
            .defaultIfEmpty(0);
    }

    //??????????????????????????????
    @GetMapping("/{deviceId:.+}/properties/latest")
    @QueryAction
    @Operation(summary = "????????????ID???????????????????????????")
    public Flux<DeviceProperty> getDeviceLatestProperties(@PathVariable @Parameter(description = "??????ID") String deviceId) {
        return deviceDataService.queryEachOneProperties(deviceId, QueryParamEntity.of());
    }

    //??????????????????????????????
    @GetMapping("/{deviceId:.+}/properties")
    @QueryAction
    @QueryNoPagingOperation(summary = "?????????????????????ID?????????????????????")
    public Flux<DeviceProperty> getDeviceLatestProperties(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                                          @Parameter(hidden = true) QueryParamEntity queryParamEntity) {
        return deviceDataService.queryEachProperties(deviceId, queryParamEntity);
    }

    //?????????????????????????????????
    @GetMapping("/{deviceId:.+}/property/{property:.+}")
    @QueryAction
    @Operation(summary = "????????????ID?????????????????????")
    public Mono<DeviceProperty> getDeviceLatestProperty(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                                        @PathVariable @Parameter(description = "??????ID") String property) {
        return deviceDataService.queryEachOneProperties(deviceId, QueryParamEntity.of(), property)
                                .take(1)
                                .singleOrEmpty()
            ;
    }

    //??????????????????
    @GetMapping("/{deviceId:.+}/property/{property}/_query")
    @QueryAction
    @QueryOperation(summary = "??????????????????????????????")
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                                                   @PathVariable @Parameter(description = "??????ID") String property,
                                                                   @Parameter(hidden = true) QueryParamEntity entity) {
        return deviceDataService.queryPropertyPage(deviceId, property, entity);
    }

    //??????????????????
    @GetMapping("/{deviceId:.+}/properties/_query")
    @QueryAction
    @QueryOperation(summary = "??????????????????????????????(?????????)")
    @Deprecated
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                                                   @Parameter(hidden = true) QueryParamEntity entity) {
        return entity
            .getTerms()
            .stream()
            .filter(term -> "property".equals(term.getColumn()))
            .findFirst()
            .map(term -> {
                String val = String.valueOf(term.getValue());
                term.setValue(null);
                return val;
            })
            .map(property -> deviceDataService.queryPropertyPage(deviceId, property, entity))
            .orElseThrow(() -> new ValidationException("?????????[property]??????"));

    }

    //????????????????????????
    @GetMapping("/{deviceId:.+}/event/{eventId}")
    @QueryAction
    @QueryOperation(summary = "????????????????????????")
    public Mono<PagerResult<DeviceEvent>> queryPagerByDeviceEvent(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                                                  @PathVariable @Parameter(description = "??????ID") String eventId,
                                                                  @Parameter(hidden = true) QueryParamEntity queryParam,

                                                                  @RequestParam(defaultValue = "false")
                                                                  @Parameter(description = "???????????????????????????,????????????????????????_format??????") boolean format) {
        return deviceDataService.queryEventPage(deviceId, eventId, queryParam, format);
    }

    //??????????????????
    @GetMapping("/{deviceId:.+}/logs")
    @QueryAction
    @QueryOperation(summary = "????????????????????????")
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                                                      @Parameter(hidden = true) QueryParamEntity entity) {
        return deviceDataService.queryDeviceMessageLog(deviceId, entity);
    }

    //????????????
    @DeleteMapping("/{deviceId}/tag/{tagId:.+}")
    @SaveAction
    @Operation(summary = "??????????????????")
    public Mono<Void> deleteDeviceTag(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                      @PathVariable @Parameter(description = "??????ID") String tagId) {
        return tagRepository.createDelete()
                            .where(DeviceTagEntity::getDeviceId, deviceId)
                            .and(DeviceTagEntity::getId, tagId)
                            .execute()
                            .then();
    }

    /**
     * ??????????????????,??????????????????????????????.
     *
     * @param idList ID??????
     * @return ???????????????
     * @since 1.1
     */
    @PutMapping("/batch/_delete")
    @DeleteAction
    @Operation(summary = "??????????????????")
    public Mono<Integer> deleteBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMapMany(Flux::fromIterable)
                     .as(service::deleteById);
    }

    /**
     * ??????????????????
     *
     * @param idList ID??????
     * @return ??????????????????
     * @since 1.1
     */
    @PutMapping("/batch/_unDeploy")
    @SaveAction
    @Operation(summary = "??????????????????")
    public Mono<Integer> unDeployBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMap(list -> service.unregisterDevice(Flux.fromIterable(list)));
    }

    /**
     * ??????????????????
     *
     * @param idList ID??????
     * @return ??????????????????
     * @since 1.1
     */
    @PutMapping("/batch/_deploy")
    @SaveAction
    @Operation(summary = "??????????????????")
    public Mono<Integer> deployBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMapMany(service::findById)
                     .as(service::deploy)
                     .map(DeviceDeployResult::getTotal)
                     .reduce(Math::addExact);
    }

    /**
     * ????????????????????????
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
     * @param deviceId ??????ID
     * @return ??????????????????
     */
    @GetMapping("/{deviceId}/tags")
    @SaveAction
    @Operation(summary = "??????????????????????????????")
    public Flux<DeviceTagEntity> getDeviceTags(@PathVariable @Parameter(description = "??????ID") String deviceId) {
        return tagRepository.createQuery()
                            .where(DeviceTagEntity::getDeviceId, deviceId)
                            .fetch();
    }

    //??????????????????
    @PatchMapping("/{deviceId}/tag")
    @SaveAction
    @Operation(summary = "??????????????????")
    public Flux<DeviceTagEntity> saveDeviceTag(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                               @RequestBody Flux<DeviceTagEntity> tags) {
        return tags
            .doOnNext(tag -> {
                tag.setId(DeviceTagEntity.createTagId(deviceId, tag.getKey()));
                tag.setDeviceId(deviceId);
                tag.tryValidate();
            })
            .as(tagRepository::save)
            .thenMany(getDeviceTags(deviceId));
    }

    DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    private Mono<Tuple4<DeviceProductEntity, DeviceProductOperator, DeviceMetadata, List<ConfigPropertyMetadata>>> getDeviceProductDetail(String productId) {

        return Mono.zip(
            //??????
            productService.findById(productId),
            //????????????
            registry.getProduct(productId),
            //?????????
            registry.getProduct(productId).flatMap(DeviceProductOperator::getMetadata),
            //??????
            metadataManager.getDeviceConfigMetadataByProductId(productId)
                           .flatMapIterable(ConfigMetadata::getProperties)
                           .collectList()
                           .defaultIfEmpty(Collections.emptyList())
        );
    }

    //?????????????????????
    @GetMapping(value = "/{productId}/import", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    @Operation(summary = "??????????????????")
    public Flux<ImportDeviceInstanceResult> doBatchImportByProduct(@PathVariable @Parameter(description = "??????ID") String productId,
                                                                   @RequestParam @Parameter(description = "????????????,??????csv,xlsx????????????") String fileUrl) {
        return Authentication
            .currentReactive()
            .flatMapMany(auth -> {

                //?????????????????????????????????????????????,???????????????????????????????????????.
                Map<String, String> orgMapping = auth
                    .getDimensions("org")
                    .stream()
                    .collect(Collectors.toMap(Dimension::getName, Dimension::getId, (_1, _2) -> _1));

                return this
                    .getDeviceProductDetail(productId)
                    .map(tp4 -> Tuples.of(new DeviceWrapper(tp4.getT3().getTags(), tp4.getT4()), tp4.getT1()))
                    .flatMapMany(wrapper -> importExportService
                        .getInputStream(fileUrl)
                        .flatMapMany(inputStream -> read(inputStream, FileUtils.getExtension(fileUrl), wrapper.getT1()))
                        .doOnNext(info -> info.setProductName(wrapper.getT2().getName()))
                    )
                    .map(info -> {
                        DeviceInstanceEntity entity = FastBeanCopier.copy(info, new DeviceInstanceEntity());
                        entity.setProductId(productId);
                        entity.setOrgId(orgMapping.get(info.getOrgName()));
                        if (StringUtils.isEmpty(entity.getId())) {
                            throw new BusinessException("???" + (info.getRowNumber() + 1) + "???:??????ID????????????");
                        }
                        return Tuples.of(entity, info.getTags());
                    })
                    .buffer(100)//???100?????????????????????
                    .publishOn(Schedulers.single())
                    .concatMap(buffer ->
                                   Mono.zip(
                                       service.save(Flux.fromIterable(buffer).map(Tuple2::getT1)),
                                       tagRepository
                                           .save(Flux.fromIterable(buffer).flatMapIterable(Tuple2::getT2))
                                           .defaultIfEmpty(SaveResult.of(0, 0))
                                   ))
                    .map(res -> ImportDeviceInstanceResult.success(res.getT1()))
                    .onErrorResume(err -> Mono.just(ImportDeviceInstanceResult.error(err)));
            });
    }

    //??????????????????
    @GetMapping("/{productId}/template.{format}")
    @QueryAction
    @Operation(summary = "????????????????????????")
    public Mono<Void> downloadExportTemplate(@PathVariable @Parameter(description = "??????ID") String productId,
                                             ServerHttpResponse response,
                                             @PathVariable @Parameter(description = "????????????,??????csv,xlsx") String format) throws IOException {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                                  "attachment; filename=".concat(URLEncoder.encode("??????????????????." + format, StandardCharsets.UTF_8
                                      .displayName())));
        return getDeviceProductDetail(productId)
            .map(tp4 -> DeviceExcelInfo.getTemplateHeaderMapping(tp4.getT3().getTags(), tp4.getT4()))
            .defaultIfEmpty(DeviceExcelInfo.getTemplateHeaderMapping(Collections.emptyList(), Collections.emptyList()))
            .flatMapMany(headers ->
                             ReactorExcel.<DeviceExcelInfo>writer(format)
                                 .headers(headers)
                                 .converter(DeviceExcelInfo::toMap)
                                 .writeBuffer(Flux.empty()))
            .doOnError(err -> log.error(err.getMessage(), err))
            .map(bufferFactory::wrap)
            .as(response::writeWith);
    }

    //????????????????????????.
    @GetMapping("/{productId}/export.{format}")
    @QueryAction
    @QueryNoPagingOperation(summary = "?????????????????????????????????")
    public Mono<Void> export(@PathVariable @Parameter(description = "??????ID") String productId,
                             ServerHttpResponse response,
                             @Parameter(hidden = true) QueryParamEntity parameter,
                             @PathVariable @Parameter(description = "????????????,??????csv,xlsx") String format) throws IOException {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                                  "attachment; filename=".concat(URLEncoder.encode("????????????." + format, StandardCharsets.UTF_8
                                      .displayName())));
        parameter.setPaging(false);
        parameter.toNestQuery(q -> q.is(DeviceInstanceEntity::getProductId, productId));
       return Authentication
            .currentReactive()
            .flatMap(auth -> {
                //?????????????????????????????????????????????,???????????????????????????????????????.
                Map<String, String> orgMapping = auth
                    .getDimensions("org")
                    .stream()
                    .collect(Collectors.toMap(Dimension::getId, Dimension::getName, (_1, _2) -> _1));
                return this
                    .getDeviceProductDetail(productId)
                    .map(tp4 -> Tuples
                        .of(
                            //??????
                            DeviceExcelInfo.getExportHeaderMapping(tp4.getT3().getTags(), tp4.getT4()),
                            //??????key??????
                            tp4
                                .getT4()
                                .stream()
                                .map(ConfigPropertyMetadata::getProperty)
                                .collect(Collectors.toList())
                        ))
                    .defaultIfEmpty(Tuples.of(DeviceExcelInfo.getExportHeaderMapping(Collections.emptyList(), Collections
                                                  .emptyList()),
                                              Collections.emptyList()))
                    .flatMapMany(headerAndConfigKey -> ReactorExcel
                        .<DeviceExcelInfo>writer(format)
                        .headers(headerAndConfigKey.getT1())
                        .converter(DeviceExcelInfo::toMap)
                        .writeBuffer(service
                                         .query(parameter)
                                         .flatMap(entity -> {
                                             DeviceExcelInfo exportEntity = FastBeanCopier.copy(entity, new DeviceExcelInfo(), "state");
                                             exportEntity.setOrgName(orgMapping.get(entity.getOrgId()));
                                             exportEntity.setState(entity.getState().getText());
                                             return registry
                                                 .getDevice(entity.getId())
                                                 .flatMap(deviceOperator -> deviceOperator
                                                     .getSelfConfigs(headerAndConfigKey.getT2())
                                                     .map(Values::getAllValues))
                                                 .doOnNext(configs -> exportEntity
                                                     .getConfiguration()
                                                     .putAll(configs))
                                                 .thenReturn(exportEntity);
                                         })
                                         .buffer(200)
                                         .flatMap(list -> {
                                             Map<String, DeviceExcelInfo> importInfo = list
                                                 .stream()
                                                 .collect(Collectors.toMap(DeviceExcelInfo::getId, Function.identity()));
                                             return tagRepository
                                                 .createQuery()
                                                 .where()
                                                 .in(DeviceTagEntity::getDeviceId, importInfo.keySet())
                                                 .fetch()
                                                 .collect(Collectors.groupingBy(DeviceTagEntity::getDeviceId))
                                                 .flatMapIterable(Map::entrySet)
                                                 .doOnNext(entry -> importInfo
                                                     .get(entry.getKey())
                                                     .setTags(entry.getValue()))
                                                 .thenMany(Flux.fromIterable(list));
                                         })
                            , 512 * 1024))//??????512k
                    .doOnError(err -> log.error(err.getMessage(), err))
                    .map(bufferFactory::wrap)
                    .as(response::writeWith);
            });
    }


    //??????????????????,?????????????????????.
    @GetMapping("/export.{format}")
    @QueryAction
    @QueryNoPagingOperation(summary = "????????????????????????", description = "???????????????????????????????????????????????????")
    public Mono<Void> export(ServerHttpResponse response,
                             @Parameter(hidden = true) QueryParamEntity parameter,
                             @PathVariable @Parameter(description = "????????????,??????csv,xlsx") String format) throws IOException {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                                  "attachment; filename=".concat(URLEncoder.encode("????????????." + format, StandardCharsets.UTF_8
                                      .displayName())));
        return ReactorExcel.<DeviceExcelInfo>writer(format)
            .headers(DeviceExcelInfo.getExportHeaderMapping(Collections.emptyList(), Collections.emptyList()))
            .converter(DeviceExcelInfo::toMap)
            .writeBuffer(
                service
                    .query(parameter)
                    .map(entity -> {
                        DeviceExcelInfo exportEntity = FastBeanCopier.copy(entity, new DeviceExcelInfo(), "state");
                        exportEntity.setState(entity.getState().getText());
                        return exportEntity;
                    })
                , 512 * 1024)//??????512k
            .doOnError(err -> log.error(err.getMessage(), err))
            .map(bufferFactory::wrap)
            .as(response::writeWith);
    }

    //??????????????????
    @PutMapping("/{deviceId:.+}/shadow")
    @SaveAction
    @Operation(summary = "??????????????????")
    public Mono<String> setDeviceShadow(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                        @RequestBody Mono<String> shadow) {
        return Mono
            .zip(registry.getDevice(deviceId), shadow)
            .flatMap(tp2 -> tp2.getT1()
                               .setConfig(DeviceConfigKey.shadow, tp2.getT2())
                               .thenReturn(tp2.getT2()));
    }

    //??????????????????
    @GetMapping("/{deviceId:.+}/shadow")
    @SaveAction
    @Operation(summary = "??????????????????")
    public Mono<String> getDeviceShadow(@PathVariable @Parameter(description = "??????ID") String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.shadow))
            .defaultIfEmpty("{\n}");
    }

    //??????????????????
    @PutMapping("/{deviceId:.+}/property")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "?????????????????????????????????", description = "????????????: {\"??????ID\":\"???\"}")
    public Flux<?> writeProperties(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                   @RequestBody Mono<Map<String, Object>> properties) {
        return properties.flatMapMany(props -> service.writeProperties(deviceId, props));
    }

    //??????????????????
    @PostMapping("/{deviceId:.+}/function/{functionId}")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "???????????????????????????????????????", description = "????????????: {\"??????\":\"???\"}")
    public Flux<?> invokedFunction(@PathVariable String deviceId,
                                   @PathVariable String functionId,
                                   @RequestBody Mono<Map<String, Object>> properties) {

        return properties.flatMapMany(props -> service.invokeFunction(deviceId, functionId, props));
    }

    @PostMapping("/{deviceId:.+}/agg/_query")
    @QueryAction
    @Operation(summary = "????????????????????????")
    public Flux<Map<String, Object>> aggDeviceProperty(@PathVariable @Parameter(description = "??????ID") String deviceId,
                                                       @RequestBody Mono<AggRequest> param) {

        return param
            .flatMapMany(request -> deviceDataService
                .aggregationPropertiesByDevice(deviceId,
                                               request.getQuery(),
                                               request
                                                   .getColumns()
                                                   .toArray(new DeviceDataService.DevicePropertyAggregation[0]))
            )
            .map(AggregationData::values);
    }

    //??????????????????
    @PostMapping("/{deviceId:.+}/message")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "?????????????????????")
    @SuppressWarnings("all")
    public Flux<?> sendMessage(@PathVariable @Parameter(description = "??????ID") String deviceId,
                               @RequestBody Mono<Map<String, Object>> properties) {
        return properties
            .flatMapMany(props -> {
                return Mono
                    .zip(
                        registry
                            .getDevice(deviceId)
                            .map(DeviceOperator::messageSender)
                            .switchIfEmpty(Mono.error(() -> new NotFoundException("???????????????????????????"))),
                        Mono.<Message>justOrEmpty(MessageType.convertMessage(props))
                            .cast(DeviceMessage.class)
                            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("????????????????????????")))
                    ).flatMapMany(tp2 -> {
                        DeviceMessageSender sender = tp2.getT1();
                        DeviceMessage message = tp2.getT2();

                        Map<String, String> copy = new HashMap<>();
                        copy.put("deviceId", deviceId);
                        if (!StringUtils.hasText(message.getMessageId())) {
                            copy.put("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
                        }
                        FastBeanCopier.copy(copy, message);
                        return sender
                            .send(message)
                            .onErrorResume(DeviceOperationException.class, error -> {
                                if (message instanceof RepayableDeviceMessage) {
                                    return Mono.just(
                                        ((RepayableDeviceMessage) message).newReply().error(error)
                                    );
                                }
                                return Mono.error(error);
                            });
                    });
            });
    }

    //??????????????????
    @PostMapping("/messages")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "???????????????????????????")
    @SuppressWarnings("all")
    public Flux<?> sendMessage(@RequestParam(required = false)
                               @Parameter(description = "???????????????????????????") String where,
                               @RequestBody Flux<Map<String, Object>> messages) {

        Lazy<Flux<DeviceOperator>> operators = Lazy.of(() -> {
            if (StringUtils.isEmpty(where)) {
                throw new ValidationException("where", "[where]??????????????????");
            }
            QueryParamEntity entity = new QueryParamEntity();
            entity.setWhere(where);
            entity.includes("id");
            return service.query(entity)
                          .flatMap(device -> registry.getDevice(device.getId()))
                          .cache();
        });
        return messages
            .flatMap(message -> {
                DeviceMessage msg = MessageType
                    .convertMessage(message)
                    .filter(DeviceMessage.class::isInstance)
                    .map(DeviceMessage.class::cast)
                    .orElseThrow(() -> new UnsupportedOperationException("????????????????????????:" + message));

                String deviceId = msg.getDeviceId();
                Flux<DeviceOperator> devices = StringUtils.isEmpty(deviceId)
                    ? operators.get()
                    : registry.getDevice(deviceId).flux();

                return devices
                    .flatMap(device -> {
                        Map<String, Object> copy = new HashMap<>(message);
                        copy.put("deviceId", device.getDeviceId());
                        copy.putIfAbsent("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
                        //?????????????????????,????????????
                        DeviceMessage copiedMessage = MessageType
                            .convertMessage(copy)
                            .map(DeviceMessage.class::cast)
                            .orElseThrow(() -> new UnsupportedOperationException("????????????????????????"));
                        return device
                            .messageSender()
                            .send(copiedMessage)
                            .onErrorResume(Exception.class, error -> {
                                if (copiedMessage instanceof RepayableDeviceMessage) {
                                    return Mono.just(
                                        ((RepayableDeviceMessage) copiedMessage).newReply().error(error)
                                    );
                                }
                                return Mono.error(error);
                            });
                    });
            });
    }

    //?????????????????????
    @PutMapping(value = "/{id}/metadata")
    @SaveAction
    @Operation(summary = "???????????????")
    public Mono<Void> updateMetadata(@PathVariable String id,
                                     @RequestBody Mono<String> metadata) {
        return metadata
            .flatMap(metadata_ -> service
                .createUpdate()
                .set(DeviceInstanceEntity::getDeriveMetadata, metadata_)
                .where(DeviceInstanceEntity::getId, id)
                .execute()
                .then(registry.getDevice(id))
                .flatMap(device -> device.updateMetadata(metadata_)))
            .then();
    }

    //?????????????????????
    @DeleteMapping(value = "/{id}/metadata")
    @SaveAction
    @Operation(summary = "???????????????")
    public Mono<Void> resetMetadata(@PathVariable String id) {

        return registry
            .getDevice(id)
            .flatMap(DeviceOperator::resetMetadata)
            .then(service
                      .createUpdate()
                      .setNull(DeviceInstanceEntity::getDeriveMetadata)
                      .where(DeviceInstanceEntity::getId, id)
                      .execute()
                      .then());
    }

    //????????????????????????
    @PutMapping(value = "/{id}/metadata/merge-product")
    @SaveAction
    @Operation(summary = "????????????????????????")
    public Mono<Void> mergeProductMetadata(@PathVariable String id) {
        return service
            .findById(id)
            //??????????????????????????????????????????
            .filter(deviceInstance -> StringUtils.hasText(deviceInstance.getDeriveMetadata()))
            .flatMap(deviceInstance -> productService
                .findById(deviceInstance.getProductId())
                .flatMap(product -> deviceInstance.mergeMetadata(product.getMetadata()))
                .then(
                    Mono.defer(() -> service
                        .createUpdate()
                        .set(deviceInstance::getDeriveMetadata)
                        .where(deviceInstance::getId)
                        .execute()
                        .then(registry.getDevice(deviceInstance.getId()))
                        .flatMap(device -> device.updateMetadata(deviceInstance.getDeriveMetadata()))
                        .then())
                ));
    }


}
