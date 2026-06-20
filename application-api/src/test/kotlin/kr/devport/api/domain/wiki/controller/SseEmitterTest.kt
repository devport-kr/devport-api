package kr.devport.api.domain.wiki.controller

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseEmitterTest {
    @Test
    fun testSse() {
        val token = "\n\n1. test\n2. test"

        println("--- Spring Default ---")
        val data1 = SseEmitter.event().name("token").data(token, MediaType.TEXT_PLAIN).build()
        for (data in data1) {
            print(data.data)
        }

        println("\n--- Custom Replace ---")
        val formatted = " " + token.replace("\n", "\ndata: ")
        val data2 = SseEmitter.event().name("token").data(formatted, MediaType.TEXT_PLAIN).build()
        for (data in data2) {
            print(data.data)
        }
    }
}
