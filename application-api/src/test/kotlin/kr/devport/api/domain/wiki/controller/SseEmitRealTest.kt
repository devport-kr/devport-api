package kr.devport.api.domain.wiki.controller

import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseEmitRealTest {
    @Test
    fun test() {
        val data1 = SseEmitter.event().name("token").data("a\nb").build()
        println("--- MULTILINE ---")
        for (d in data1) {
            println("Line: [" + d.data.toString().replace("\n", "\\n") + "]")
        }
    }
}
