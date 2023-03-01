package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.device.entity.DeviceGeoEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceProductState;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.jetlinks.core.device.DeviceRegistry;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author luo'xing'yue
 * @CreateTime 2022-08-13
 */
@Service
@Slf4j
public class DeviceLocationService extends GenericReactiveCrudService<DeviceGeoEntity, String> {
    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private LocalDeviceInstanceService service;

    @Autowired
    private ReactiveRepository<DeviceGeoEntity, String> geoRepository;


    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
            .collectList()
            .flatMap(idList ->
                geoRepository.createQuery()
                    .where()
                    .in(DeviceGeoEntity::getId, idList)
                    .count()
                    .flatMap(i -> {
                        if (i > 0) {
                            return Mono.error(new IllegalArgumentException("存在关联区域,无法删除!"));
                        } else {
                            return super.deleteById(Flux.fromIterable(idList));
                        }
                    }));
    }
}
