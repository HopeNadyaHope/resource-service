package com.epam.microservices.controller;

import com.epam.microservices.service.RabbitMQSender;
import com.epam.microservices.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/resources")
public class ResourceController {
    private static final String ID = "id";
    private static final String RANGE = "range";
    private static final String RANGE_SEPARATOR = "( - )";
    @Autowired
    private ResourceService service;
    @Autowired
    private RabbitMQSender rabbitMQSender;

    @PostMapping(produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody Map<String, Integer> create(@RequestParam("file") MultipartFile file) {
        int createdResourceId = service.create(file);
        rabbitMQSender.sendUploadedResourceId(createdResourceId);
        return Map.of(ID, createdResourceId);
    }

    @GetMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody ResponseEntity<byte[]> read(@PathVariable(name = "id") Integer id,
                                                     @RequestHeader(required = false) Map<String, String> headers) {

        byte[] fileBytes;
        ResponseEntity<byte[]> responseEntity;

        if (!headers.containsKey(RANGE)) {
            fileBytes = service.getFileBytes(id);
            responseEntity = new ResponseEntity<>(fileBytes, HttpStatus.OK);
        } else {
            List<Integer> range = Arrays.stream(headers.get(RANGE).split(RANGE_SEPARATOR))
                    .map(Integer::parseInt)
                    .collect(toList());
            fileBytes = service.getFileBytes(id, range);
            responseEntity = new ResponseEntity<>(fileBytes, HttpStatus.PARTIAL_CONTENT);
        }

        return responseEntity;
    }

    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody Map<String, List<Integer>> delete(@PathVariable(name = "id") List<Integer> ids) {
        return Map.of(ID, service.delete(ids));
    }

    @GetMapping(value = "/permanent/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void permanentResource(@PathVariable(name = "id") Integer id) {
        service.permanentResource(id);
    }

}
