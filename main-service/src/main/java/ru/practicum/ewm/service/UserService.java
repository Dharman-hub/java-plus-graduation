package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.UsersGetParams;

import java.util.List;

public interface UserService {

    List<UserDto> getUsers(UsersGetParams params);

    UserDto addUser(NewUserRequest request);

    void deleteUser(Long userId);
}