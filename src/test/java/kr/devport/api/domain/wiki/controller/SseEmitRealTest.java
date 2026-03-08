package kr.devport.api.domain.wiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public class SseEmitRealTest {
    @Test
    public void test() throws Exception {
        Set<ResponseBodyEmitter.DataWithMediaType> data1 = SseEmitter.event().name("token").data("a\nb").build();
        System.out.println("--- MULTILINE ---");
        for (ResponseBodyEmitter.DataWithMediaType d : data1) {
            System.out.println("Line: [" + d.getData().toString().replace("\n", "\\n") + "]");
        }
    }
}
