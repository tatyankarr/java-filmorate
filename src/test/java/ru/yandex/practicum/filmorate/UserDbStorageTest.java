package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    @Test
    @DisplayName("Создание и получение пользователя по ID")
    void testCreateAndFindUserById() {
        User user = new User();
        user.setEmail("new@mail.com");
        user.setLogin("newUser");
        user.setName("New User");
        user.setBirthday(LocalDate.of(1995, 5, 10));

        User created = userStorage.create(user);

        Optional<User> found = userStorage.findById(created.getId());

        assertThat(found)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getEmail()).isEqualTo("new@mail.com");
                    assertThat(u.getLogin()).isEqualTo("newUser");
                    assertThat(u.getName()).isEqualTo("New User");
                    assertThat(u.getBirthday()).isEqualTo(LocalDate.of(1995, 5, 10));
                });
    }

    @Test
    @DisplayName("Обновление пользователя")
    void testUpdateUser() {
        User user = new User();
        user.setEmail("original@mail.com");
        user.setLogin("origUser");
        user.setName("Original");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User created = userStorage.create(user);

        created.setName("Updated Name");
        created.setEmail("updated@mail.com");

        userStorage.update(created);

        Optional<User> updated = userStorage.findById(created.getId());

        assertThat(updated)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getName()).isEqualTo("Updated Name");
                    assertThat(u.getEmail()).isEqualTo("updated@mail.com");
                });
    }

    @Test
    @DisplayName("Удаление пользователя")
    void testDeleteUser() {
        User user = new User();
        user.setEmail("delete@mail.com");
        user.setLogin("toDelete");
        user.setName("Temp");
        user.setBirthday(LocalDate.of(1999, 9, 9));

        User created = userStorage.create(user);

        userStorage.deleteById(created.getId());

        Optional<User> found = userStorage.findById(created.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Получение всех пользователей")
    void testFindAllUsers() {
        userStorage.clear();

        User u1 = new User();
        u1.setEmail("a@mail.com");
        u1.setLogin("a");
        u1.setName("User A");
        u1.setBirthday(LocalDate.of(1980, 1, 1));

        User u2 = new User();
        u2.setEmail("b@mail.com");
        u2.setLogin("b");
        u2.setName("User B");
        u2.setBirthday(LocalDate.of(1990, 2, 2));

        userStorage.create(u1);
        userStorage.create(u2);

        Collection<User> users = userStorage.findAll();

        assertThat(users)
                .hasSize(2)
                .extracting(User::getLogin)
                .containsExactlyInAnyOrder("a", "b");
    }
}
