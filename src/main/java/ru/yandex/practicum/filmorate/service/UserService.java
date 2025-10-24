package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        return userStorage.create(user);
    }

    public User update(User user) {
        return userStorage.update(user);
    }

    public void deleteUser(Long id) {
        userStorage.deleteById(id);
    }

    public void clearAllUsers() {
        userStorage.clear();
    }

    public User addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может добавить самого себя в друзья");
        }

        User user = getUserById(userId);
        User friendUser = getUserById(friendId);

        user.getFriends().add(friendId);
        friendUser.getFriends().add(userId);

        return user;
    }

    public User removeFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friendUser = getUserById(friendId);

        user.getFriends().remove(friendId);
        friendUser.getFriends().remove(userId);

        return user;
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        User user = getUserById(userId);
        User other = getUserById(otherId);

        Set<Long> commonIds = new HashSet<>(user.getFriends());
        commonIds.retainAll(other.getFriends());

        return commonIds.stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public List<User> getFriends(Long userId) {
        User user = getUserById(userId);
        return user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    private User getUserById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }
}
