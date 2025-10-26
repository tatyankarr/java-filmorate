package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public Collection<User> findAll() {
        log.debug("Получен запрос на получение всех пользователей");
        return userStorage.findAll();
    }

    public User create(User user) {
        log.debug("Создание пользователя: {}", user);
        return userStorage.create(user);
    }

    public User update(User user) {
        log.debug("Обновление пользователя: {}", user);
        return userStorage.update(user);
    }

    public void deleteUser(Long id) {
        log.debug("Удаление пользователя с id={}", id);
        userStorage.deleteById(id);
    }

    public void clearAllUsers() {
        log.debug("Очистка всех пользователей");
        userStorage.clear();
    }

    public User addFriend(Long userId, Long friendId) {
        log.debug("Добавление друга: userId={} friendId={}", userId, friendId);

        if (userId.equals(friendId)) {
            log.warn("Попытка добавить самого себя в друзья: userId={}", userId);
            throw new ValidationException("Пользователь не может добавить самого себя в друзья");
        }

        User user = getUserById(userId);
        User friendUser = getUserById(friendId);

        user.getFriends().add(friendId);
        friendUser.getFriends().add(userId);

        log.trace("Пользователь '{}' теперь друзья с '{}'", user.getName(), friendUser.getName());
        return user;
    }

    public User removeFriend(Long userId, Long friendId) {
        log.debug("Удаление друга: userId={} friendId={}", userId, friendId);

        User user = getUserById(userId);
        User friendUser = getUserById(friendId);

        user.getFriends().remove(friendId);
        friendUser.getFriends().remove(userId);

        log.trace("Пользователь '{}' больше не является другом '{}'", user.getName(), friendUser.getName());
        return user;
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.debug("Запрос общих друзей: userId={} otherId={}", userId, otherId);

        User user = getUserById(userId);
        User other = getUserById(otherId);

        Set<Long> commonIds = new HashSet<>(user.getFriends());
        commonIds.retainAll(other.getFriends());

        List<User> commonFriends = commonIds.stream()
                .map(this::getUserById)
                .collect(Collectors.toList());

        log.trace("Общие друзья пользователей '{}' и '{}': {}", user.getName(), other.getName(),
                commonFriends.stream().map(User::getName).collect(Collectors.toList()));
        return commonFriends;
    }

    public List<User> getFriends(Long userId) {
        log.debug("Запрос списка друзей пользователя id={}", userId);
        User user = getUserById(userId);

        List<User> friends = user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());

        log.trace("Список друзей пользователя '{}': {}", user.getName(),
                friends.stream().map(User::getName).collect(Collectors.toList()));
        return friends;
    }

    private User getUserById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", id);
                    return new NotFoundException("Пользователь с id=" + id + " не найден");
                });
    }
}
