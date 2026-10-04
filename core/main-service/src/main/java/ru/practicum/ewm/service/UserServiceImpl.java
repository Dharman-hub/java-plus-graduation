package ru.practicum.ewm.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dao.UserRepository;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.UsersGetParams;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.QUser;
import ru.practicum.ewm.model.User;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserDto> getUsers(UsersGetParams params) {
        log.info("Получение пользователей по заданным параметрам (ids={}, from={}, size={})",
                params.getIds(), params.getFrom(), params.getSize());
        int from = params.getFrom();
        int size = params.getSize();

        Pageable pageable = PageRequest.of(0, from + size);
        BooleanExpression predicate = buildPredicate(params);
        Page<User> page = userRepository.findAll(predicate, pageable);

        return page.getContent().stream()
                .skip(from)
                .limit(size)
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UserDto addUser(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с email=" + request.getEmail() + " уже существует");
        }

        User user = UserMapper.toUser(request);
        User savedUser = userRepository.save(user);

        return UserMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        userRepository.deleteById(userId);
    }

    private BooleanExpression buildPredicate(UsersGetParams params) {
        QUser user = QUser.user;
        BooleanExpression predicate = null;

        if (params.getIds() != null && !params.getIds().isEmpty()) {
            predicate = user.id.in(params.getIds());
        }

        return predicate == null ? user.isNotNull() : predicate;
    }
}