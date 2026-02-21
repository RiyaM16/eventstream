package com.eventstream;

import com.eventstream.controller.EventController;
import com.eventstream.dto.EventRequest;
import com.eventstream.publisher.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean EventPublisher eventPublisher;

    @Test
    void returns202_whenValid() throws Exception {
        doNothing().when(eventPublisher).publish(any());
        EventRequest req = new EventRequest();
        req.setEventType("ORDER_PLACED");
        req.setSource("order-service");
        req.setPayload(Map.of("orderId", "123"));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.eventId").isNotEmpty());
    }

    @Test
    void returns400_whenEventTypeMissing() throws Exception {
        EventRequest req = new EventRequest();
        req.setSource("order-service");
        req.setPayload(Map.of("k", "v"));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
