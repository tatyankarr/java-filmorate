package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public User create(User user) {
        validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"user_id"});
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);

        user.setId(keyHolder.getKey().longValue());
        log.info("Добавлен пользователь '{}' (id={})", user.getLogin(), user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        validateUser(user);

        String sql = "UPDATE users SET email=?, login=?, name=?, birthday=? WHERE user_id=?";
        int updated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId());

        if (updated == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
        log.info("Пользователь с id={} обновлён", user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);
        return users.stream().findFirst();
    }

    @Override
    public void deleteById(Long id) {
        int deleted = jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
        if (deleted == 0) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        log.info("Пользователь с id={} удалён", id);
    }

    @Override
    public void clear() {
        jdbcTemplate.update("DELETE FROM users");
        log.info("Все пользователи удалены");
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT EXISTS (SELECT 1 FROM users WHERE user_id = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, id);
        return Boolean.TRUE.equals(exists);
    }

    private User mapRowToUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    }

    public void addFriend(Long userId, Long friendId) {
        String checkSql = "SELECT COUNT(*) FROM friendship WHERE user_id=? AND friend_id=?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);
        if (count != null && count > 0) return;

        String sql = "INSERT INTO friendship (user_id, friend_id, status) VALUES (?, ?, TRUE)";
        jdbcTemplate.update(sql, userId, friendId);

        log.info("Пользователь {} добавил пользователя {} в друзья (односторонняя)", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendship WHERE user_id=? AND friend_id=?";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} удалил пользователя {} из друзей (если был)", userId, friendId);
    }


    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status = TRUE";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f1 ON u.user_id = f1.friend_id AND f1.user_id = ? AND f1.status = TRUE " +
                "JOIN friendship f2 ON u.user_id = f2.friend_id AND f2.user_id = ? AND f2.status = TRUE";

        return jdbcTemplate.query(sql, this::mapRowToUser, userId, otherUserId);
    }

    private void validateUser(User user) {
        validateEmail(user.getEmail());
        validateLogin(user.getLogin());
        validateBirthday(user.getBirthday());
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) throw new ValidationException("Электронная почта не может быть пустой");
        if (!email.contains("@")) throw new ValidationException("Электронная почта должна содержать символ @");
    }

    private void validateLogin(String login) {
        if (login == null || login.isBlank()) throw new ValidationException("Логин не может быть пустым");
        if (login.contains(" ")) throw new ValidationException("Логин не может содержать пробелы");
    }

    private void validateBirthday(LocalDate birthday) {
        if (birthday == null) throw new ValidationException("Дата рождения не может быть пустой");
        if (birthday.isAfter(LocalDate.now())) throw new ValidationException("Дата рождения не может быть в будущем");
    }
}

