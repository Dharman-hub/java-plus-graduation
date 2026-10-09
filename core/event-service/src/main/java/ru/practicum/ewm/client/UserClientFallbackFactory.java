package ru.practicum.ewm.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.UserDto;

import java.util.List;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {

            @Override
            public UserDto getUser(Long userId) {
                rethrowClientError(cause);

                return UserDto.builder()
                        .id(userId)
                        .name("Unknown")
                        .email("")
                        .build();
            }

            @Override
            public List<UserDto> getUsers(List<Long> ids) {
                rethrowClientError(cause);

                return ids.stream()
                        .map(id -> UserDto.builder()
                                .id(id)
                                .name("Unknown")
                                .email("")
                                .build())
                        .toList();
            }
        };
    }

    private void rethrowClientError(Throwable cause) {
        if (cause instanceof FeignException exception
                && exception.status() >= 400
                && exception.status() < 500) {
            throw exception;
        }
    }
}