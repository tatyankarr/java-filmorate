package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerTest {

    private UserController userController;
    private UserStorage userStorage;
    private UserService userService;

    private User createValidUser() {
        User user = new User();
        user.setEmail("user@yandex.by");
        user.setLogin("login");
        user.setName("User Name");
        user.setBirthday(LocalDate.of(2005, 12, 6));
        return user;
    }

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        userController = new UserController(userService);
    }

    @Test
    void createUserWithValidDataShouldCreateUser() {
        User user = createValidUser();
        User createdUser = userController.create(user);
        assertNotNull(createdUser.getId());
        assertEquals("user@yandex.by", createdUser.getEmail());
        assertEquals("login", createdUser.getLogin());
        assertEquals("User Name", createdUser.getName());
        assertEquals(LocalDate.of(2005, 12, 6), createdUser.getBirthday());
    }

    @Test
    void createUserWithEmptyNameShouldUseLoginAsName() {
        User user = createValidUser();
        user.setName("");
        User createdUser = userController.create(user);
        assertEquals("login", createdUser.getName());
    }

    @Test
    void createUserWithNullNameShouldUseLoginAsName() {
        User user = createValidUser();
        user.setName(null);
        User createdUser = userController.create(user);
        assertEquals("login", createdUser.getName());
    }

    @Test
    void createUserWithBlankEmailShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Электронная почта не может быть пустой", exception.getMessage());
    }

    @Test
    void createUserWithNullEmailShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Электронная почта не может быть пустой", exception.getMessage());
    }

    @Test
    void createUserWithInvalidEmailFormatShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail("invalid-email");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Электронная почта должна содержать символ @", exception.getMessage());
    }

    @Test
    void createUserWithBlankLoginShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может быть пустым", exception.getMessage());
    }

    @Test
    void createUserWithNullLoginShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может быть пустым", exception.getMessage());
    }

    @Test
    void createUserWithLoginContainingSpacesShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin("login with spaces");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может содержать пробелы", exception.getMessage());
    }

    @Test
    void createUserWithNullBirthdayShouldThrowValidationException() {
        User user = createValidUser();
        user.setBirthday(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Дата рождения не может быть пустой", exception.getMessage());
    }

    @Test
    void createUserWithFutureBirthdayShouldThrowValidationException() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Дата рождения не может быть в будущем", exception.getMessage());
    }

    @Test
    void createUserWithTodayBirthdayShouldCreateUser() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now());
        User createdUser = userController.create(user);
        assertNotNull(createdUser);
        assertEquals(LocalDate.now(), createdUser.getBirthday());
    }

    @Test
    void updateUserWithValidDataShouldUpdateUser() {
        User existingUser = userController.create(createValidUser());
        User updateUser = createValidUser();
        updateUser.setId(existingUser.getId());
        updateUser.setName("Updated Name");
        updateUser.setEmail("updated@yandex.by");

        User updatedUser = userController.update(updateUser);

        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updated@yandex.by", updatedUser.getEmail());
        assertEquals(existingUser.getId(), updatedUser.getId());
    }

    @Test
    void updateUserWithNonExistentIdShouldThrowNotFoundException() {
        User user = createValidUser();
        user.setId(999L);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userController.update(user));
        assertEquals("Пользователь с id = 999 не найден", exception.getMessage());
    }

    @Test
    void updateUserWithNullIdShouldThrowNotFoundException() {
        User user = createValidUser();
        user.setId(null);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userController.update(user));
        assertEquals("Пользователь с id = null не найден", exception.getMessage());
    }

    @Test
    void updateUserWithPartialUpdateShouldUpdateOnlyProvidedFields() {
        User existingUser = userController.create(createValidUser());
        User updateUser = new User();
        updateUser.setId(existingUser.getId());
        updateUser.setName("Only Name Updated");

        User updatedUser = userController.update(updateUser);

        assertEquals("Only Name Updated", updatedUser.getName());
        assertEquals(existingUser.getEmail(), updatedUser.getEmail());
        assertEquals(existingUser.getLogin(), updatedUser.getLogin());
        assertEquals(existingUser.getBirthday(), updatedUser.getBirthday());
    }

    @Test
    void updateUserWithEmptyNameShouldSetLoginAsName() {
        User existingUser = userController.create(createValidUser());
        User updateUser = new User();
        updateUser.setId(existingUser.getId());
        updateUser.setName("");
        User updatedUser = userController.update(updateUser);
        assertEquals(existingUser.getLogin(), updatedUser.getName());
    }

    @Test
    void findAllShouldReturnAllUsers() {
        userController.create(createValidUser());
        userController.create(createValidUser());
        var users = userController.findAll();
        assertEquals(2, users.size());
    }
}
