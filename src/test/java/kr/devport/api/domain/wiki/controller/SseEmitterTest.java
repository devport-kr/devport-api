package kr.devport.api.domain.wiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public class SseEmitterTest {

    @Test
    public void testSse() throws Exception {
        String token = "\n\n1. test\n2. test";
        
        System.out.println("--- Spring Default ---");
        Set<ResponseBodyEmitter.DataWithMediaType> data1 = SseEmitter.event().name("token").data(token, MediaType.TEXT_PLAIN).build();
        for (ResponseBodyEmitter.DataWithMediaType data : data1) {
            System.out.print(data.getData());
        }

        System.out.println("\n--- Custom Replace ---");
        String formatted = " " + token.replace("\n", "\ndata: ");
        Set<ResponseBodyEmitter.DataWithMediaType> data2 = SseEmitter.event().name("token").data(formatted, MediaType.TEXT_PLAIN).build();
        for (ResponseBodyEmitter.DataWithMediaType data : data2) {
            System.out.print(data.getData());
        }
    }
}
