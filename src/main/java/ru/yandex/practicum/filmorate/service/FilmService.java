package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Collection<Film> findAll() {
        log.debug("Получен запрос на получение всех фильмов");
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        log.debug("Создание фильма: {}", film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        log.debug("Обновление фильма: {}", film);
        return filmStorage.update(film);
    }

    public void deleteFilm(Long id) {
        log.debug("Удаление фильма с id={}", id);
        filmStorage.deleteById(id);
    }

    public void clearAllFilms() {
        log.debug("Очистка всех фильмов");
        filmStorage.clear();
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка фильму id={} от пользователя id={}", filmId, userId);
        Film film = getFilmById(filmId);
        validateUserExists(userId);
        film.getLikes().add(userId);
        log.trace("Фильм '{}' теперь имеет {} лайков", film.getName(), film.getLikes().size());
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка у фильма id={} от пользователя id={}", filmId, userId);
        Film film = getFilmById(filmId);
        validateUserExists(userId);
        film.getLikes().remove(userId);
        log.trace("Фильм '{}' теперь имеет {} лайков", film.getName(), film.getLikes().size());
    }

    public List<Film> getMostPopular(int count) {
        log.debug("Запрос популярных фильмов, количество={}", count);
        List<Film> popularFilms = filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
        log.trace("Топ {} популярных фильмов: {}", count,
                popularFilms.stream().map(Film::getName).collect(Collectors.toList()));
        return popularFilms;
    }

    private void validateUserExists(Long userId) {
        boolean exists = userStorage.findAll().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (!exists) {
            log.warn("Попытка обращения к несуществующему пользователю id={}", userId);
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private Film getFilmById(Long filmId) {
        return filmStorage.findById(filmId)
                .orElseThrow(() -> {
                    log.warn("Фильм с id={} не найден", filmId);
                    return new NotFoundException("Фильм с id=" + filmId + " не найден");
                });
    }
}
