package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @GetMapping
    public Collection<Film> findAll() {
        log.info("Запрошен список всех фильмов. Количество: {}", films.size());
        return films.values();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Добавлен новый фильм: {} (id={})", film.getName(), film.getId());
        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
        if (newFilm.getId() == null || !films.containsKey(newFilm.getId())) {
            log.warn("Ошибка обновления: фильм с id={} не найден", newFilm.getId());
            throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
        }

        Film oldFilm = films.get(newFilm.getId());

        if (newFilm.getName() != null) {
            validateName(newFilm.getName());
            oldFilm.setName(newFilm.getName());
        }

        if (newFilm.getDescription() != null) {
            validateDescription(newFilm.getDescription());
            oldFilm.setDescription(newFilm.getDescription());
        }

        if (newFilm.getReleaseDate() != null) {
            validateReleaseDate(newFilm.getReleaseDate());
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
        }

        if (newFilm.getDuration() != null) {
            validateDuration(newFilm.getDuration());
            oldFilm.setDuration(newFilm.getDuration());
        }

        log.info("Фильм с id={} обновлён", oldFilm.getId());
        return oldFilm;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            log.error("Ошибка валидации: название пустое");
            throw new ValidationException("Название не может быть пустым");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > 200) {
            log.error("Ошибка валидации: описание превышает 200 символов");
            throw new ValidationException("Максимальная длина описания составляет 200 символов");
        }
    }

    private void validateReleaseDate(LocalDate releaseDate) {
        if (releaseDate == null) {
            log.error("Ошибка валидации: дата релиза пустая");
            throw new ValidationException("Дата релиза не может быть пустой");
        }
        if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
            log.error("Ошибка валидации: дата релиза {} раньше допустимой {}", releaseDate, MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }

    private void validateDuration(Integer duration) {
        if (duration == null) {
            log.error("Ошибка валидации: продолжительность пустая");
            throw new ValidationException("Продолжительность фильма не может быть пустой");
        }
        if (duration <= 0) {
            log.error("Ошибка валидации: продолжительность {} не положительная", duration);
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    private void validateFilm(Film film) {
        validateName(film.getName());
        validateDescription(film.getDescription());
        validateReleaseDate(film.getReleaseDate());
        validateDuration(film.getDuration());
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
