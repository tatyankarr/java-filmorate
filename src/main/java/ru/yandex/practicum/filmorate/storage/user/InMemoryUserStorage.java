package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component("inMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();

    @Override
    public Collection<User> findAll() {
        log.info("Список всех пользователей. Количество: {}", users.size());
        return users.values();
    }

    @Override
    public User create(User user) {
        validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Создан пользователь: {} (id={})", user.getName(), user.getId());
        return user;
    }

    @Override
    public User update(User newUser) {
        if (newUser.getId() == null || !users.containsKey(newUser.getId())) {
            throw new NotFoundException("Пользователь с id=" + newUser.getId() + " не найден");
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
            oldUser.setName(newUser.getName().isBlank() ? oldUser.getLogin() : newUser.getName());
        }

        if (newUser.getBirthday() != null) {
            validateBirthday(newUser.getBirthday());
            oldUser.setBirthday(newUser.getBirthday());
        }

        return oldUser;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void deleteById(Long id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        users.remove(id);
    }

    @Override
    public void clear() {
        users.clear();
    }

    public void addFriend(Long userId, Long friendId) {
        User user = getUser(userId);
        User friend = getUser(friendId);
        user.getFriends().add(friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = getUser(userId);
        User friend = getUser(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
    }

    public List<User> getFriends(Long userId) {
        User user = getUser(userId);
        List<User> friends = new ArrayList<>();
        for (Long fid : user.getFriends()) {
            User friend = users.get(fid);
            if (friend != null) {
                friends.add(friend);
            }
        }
        return friends;
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        User user1 = getUser(userId);
        User user2 = getUser(otherUserId);

        Set<Long> commonIds = new HashSet<>(user1.getFriends());
        commonIds.retainAll(user2.getFriends());

        List<User> common = new ArrayList<>();
        for (Long id : commonIds) {
            User u = users.get(id);
            if (u != null) {
                common.add(u);
            }
        }
        return common;
    }

    private void validateUser(User user) {
        validateEmail(user.getEmail());
        validateLogin(user.getLogin());
        validateBirthday(user.getBirthday());
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank())
            throw new ValidationException("Электронная почта не может быть пустой");
        if (!email.contains("@"))
            throw new ValidationException("Электронная почта должна содержать символ @");
    }

    private void validateLogin(String login) {
        if (login == null || login.isBlank())
            throw new ValidationException("Логин не может быть пустым");
        if (login.contains(" "))
            throw new ValidationException("Логин не может содержать пробелы");
    }

    private void validateBirthday(LocalDate birthday) {
        if (birthday == null)
            throw new ValidationException("Дата рождения не может быть пустой");
        if (birthday.isAfter(LocalDate.now()))
            throw new ValidationException("Дата рождения не может быть в будущем");
    }

    private User getUser(Long id) {
        User user = users.get(id);
        if (user == null) throw new NotFoundException("Пользователь с id=" + id + " не найден");
        return user;
    }

    private long getNextId() {
        return users.keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1;
    }
}
