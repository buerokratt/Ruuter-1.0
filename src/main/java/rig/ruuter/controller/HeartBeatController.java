package rig.ruuter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rig.ruuter.model.HeartBeatInfo;
import rig.ruuter.service.HeartBeatService;

@Slf4j
@RestController
public class HeartBeatController {

    public static final String URL = "/healthz";

    private final HeartBeatService heartBeatService;

    public HeartBeatController(HeartBeatService heartBeatService) {
        this.heartBeatService = heartBeatService;
    }

    @RequestMapping(value = URL)
    public ResponseEntity<HeartBeatInfo> getData() {
        return ResponseEntity.ok().body(heartBeatService.getData());
    }

}
