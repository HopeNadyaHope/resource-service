package com.epam.microservices.client;

import com.epam.microservices.model.StorageModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "api-gateway-service")
public interface ApiGatewayClient {

    @GetMapping(value = "/storages")
    List<StorageModel> getStorages();
}
