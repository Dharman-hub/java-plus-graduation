package ru.practicum.ewm.client.review;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.UserDto;

import java.util.List;

@FeignClient(name = "user-service", contextId = "reviewUserClient")
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUser(@PathVariable Long userId);

    @GetMapping("/internal/users")
    List<UserDto> getUsers(@RequestParam List<Long> ids);
}