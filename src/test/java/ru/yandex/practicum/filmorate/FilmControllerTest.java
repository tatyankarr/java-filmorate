package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FilmControllerTest {

    private FilmController filmController;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895,12,28);

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Film Name");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Test
    void createFilmWithValidDataShouldCreateFilm() {
        Film film = createValidFilm();
        Film createdFilm = filmController.create(film);
        assertNotNull(createdFilm.getId());
        assertEquals("Film Name", createdFilm.getName());
        assertEquals("Description", createdFilm.getDescription());
        assertEquals(LocalDate.of(2000, 1, 1), createdFilm.getReleaseDate());
        assertEquals(120, createdFilm.getDuration());
    }

    @Test
    void createFilmWithEmptyNameShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setName("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Название не может быть пустым", exception.getMessage());
    }

    @Test
    void createFilmWithNullNameShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setName(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Название не может быть пустым", exception.getMessage());
    }

    @Test
    void createFilmWithLongDescriptionShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(201));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Максимальная длина описания составляет 200 символов", exception.getMessage());
    }

    @Test
    void createFilmWith200SymbolDescriptionShouldCreateFilm() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(200));
        Film createdFilm = filmController.create(film);
        assertNotNull(createdFilm);
        assertEquals(200, createdFilm.getDescription().length());
    }

    @Test
    void createFilmWithNullDescriptionShouldCreateFilm() {
        Film film = createValidFilm();
        film.setDescription(null);
        Film createdFilm = filmController.create(film);
        assertNotNull(createdFilm);
        assertNull(createdFilm.getDescription());
    }

    @Test
    void createFilmWithReleaseDateBeforeMinDateShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setReleaseDate(MIN_RELEASE_DATE.minusDays(1));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", exception.getMessage());
    }

    @Test
    void createFilmWithMinReleaseDateShouldCreateFilm() {
        Film film = createValidFilm();
        film.setReleaseDate(MIN_RELEASE_DATE);
        Film createdFilm = filmController.create(film);
        assertNotNull(createdFilm);
        assertEquals(MIN_RELEASE_DATE, createdFilm.getReleaseDate());
    }

    @Test
    void createFilm_WithNullReleaseDateShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setReleaseDate(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Дата релиза не может быть пустой", exception.getMessage());
    }

    @Test
    void createFilmWithZeroDurationShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDuration(0);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void createFilmWithNegativeDurationShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDuration(-10);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void createFilmWithNullDurationShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDuration(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма не может быть пустой", exception.getMessage());
    }

    @Test
    void updateFilmWithValidDataShouldUpdateFilm() {
        Film existingFilm = filmController.create(createValidFilm());
        Film updateFilm = createValidFilm();
        updateFilm.setId(existingFilm.getId());
        updateFilm.setName("Updated Film Name");
        updateFilm.setDuration(150);
        Film updatedFilm = filmController.update(updateFilm);

        assertEquals("Updated Film Name", updatedFilm.getName());
        assertEquals(150, updatedFilm.getDuration());
        assertEquals(existingFilm.getId(), updatedFilm.getId());
    }

    @Test
    void updateFilmWithNonExistentIdShouldThrowNotFoundException() {
        Film film = createValidFilm();
        film.setId(999L);
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmController.update(film));
        assertEquals("Фильм с id = 999 не найден", exception.getMessage());
    }

    @Test
    void updateFilmWithNullIdShouldThrowNotFoundException() {
        Film film = createValidFilm();
        film.setId(null);
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmController.update(film));
        assertEquals("Фильм с id = null не найден", exception.getMessage());
    }

    @Test
    void updateFilmWithPartialUpdateShouldUpdateOnlyProvidedFields() {
        Film existingFilm = filmController.create(createValidFilm());
        Film updateFilm = new Film();
        updateFilm.setId(existingFilm.getId());
        updateFilm.setName("Only Name Updated");
        Film updatedFilm = filmController.update(updateFilm);

        assertEquals("Only Name Updated", updatedFilm.getName());
        assertEquals(existingFilm.getDescription(), updatedFilm.getDescription());
        assertEquals(existingFilm.getReleaseDate(), updatedFilm.getReleaseDate());
        assertEquals(existingFilm.getDuration(), updatedFilm.getDuration());
    }

    @Test
    void findAllShouldReturnAllFilms() {
        filmController.create(createValidFilm());
        filmController.create(createValidFilm());
        var films = filmController.findAll();
        assertEquals(2, films.size());
    }
}
