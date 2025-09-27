package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> findAll() {
        log.info("Запрошен список всех пользователей. Количество: {}", users.size());
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Добавлен новый пользователь: {} (id={}", user.getName(), user.getId());
        return user;
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        if (newUser.getId() == null || !users.containsKey(newUser.getId())) {
            log.warn("Ошибка обновления: пользователь с id={} не найден", newUser.getId());
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }

        User oldUser = users.get(newUser.getId());

        if (newUser.getEmail() != null) {
            validateEmail(newUser.getEmail());
            oldUser.setEmail(newUser.getEmail());
        }

        if (newUser.getLogin() != null) {
            validateLogin(newUser.getLogin());
            oldUser.setLogin(newUser.getLogin());
        }

        if (newUser.getName() != null) {
            if (newUser.getName().isBlank()) {
                oldUser.setName(oldUser.getLogin());
            } else {
                oldUser.setName(newUser.getName());
            }
        }

        if (newUser.getBirthday() != null) {
            validateBirthday(newUser.getBirthday());
            oldUser.setBirthday(newUser.getBirthday());
        }

        log.info("Пользователь с id={} обновлён", oldUser.getId());
        return oldUser;
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            log.error("Ошибка валидации: электронная почта пустая");
            throw new ValidationException("Электронная почта не может быть пустой");
        }
        if (!email.contains("@")) {
            log.error("Ошибка валидации: электронная почта {} не содержит @", email);
            throw new ValidationException("Электронная почта должна содержать символ @");
        }
    }

    private void validateLogin(String login) {
        if (login == null || login.isBlank()) {
            log.error("Ошибка валидации: логин пустой");
            throw new ValidationException("Логин не может быть пустым");
        }
        if (login.contains(" ")) {
            log.error("Ошибка валидации: логин {} содержит пробелы", login);
            throw new ValidationException("Логин не может содержать пробелы");
        }
    }

    private void validateBirthday(LocalDate birthday) {
        if (birthday.isAfter(LocalDate.now())) {
            log.error("Ошибка валидации: дата рождения {} позже текущей {}", birthday, LocalDate.now());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void validateUser(User user) {
        validateEmail(user.getEmail());
        validateLogin(user.getLogin());
        validateBirthday(user.getBirthday());
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
