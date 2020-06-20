package com.herocc.school.aspencheck.aspen;

import com.herocc.school.aspencheck.AspenCheck;
import com.herocc.school.aspencheck.District;
import com.herocc.school.aspencheck.ErrorInfo;
import com.herocc.school.aspencheck.JSONReturn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@CrossOrigin
@RestController
@RequestMapping("/{district-id}/aspen")
public class AspenController {

    @Async
    @RequestMapping("/checkLogin")
    public CompletableFuture<ResponseEntity<JSONReturn>> checkLogin(@PathVariable(value = "district-id") String districtName,
                                                                   @RequestHeader(value = "ASPEN_UNAME") String u,
                                                                   @RequestHeader(value = "ASPEN_PASS") String p) {

        AspenWebFetch a = new AspenWebFetch(districtName, u, p);
        AspenCheck.log.info("aspenWebFetch object" + a.toString());
        return CompletableFuture.completedFuture(new ResponseEntity<>(new JSONReturn(a.areCredsCorrect(), new ErrorInfo()), HttpStatus.OK));
    }

}
