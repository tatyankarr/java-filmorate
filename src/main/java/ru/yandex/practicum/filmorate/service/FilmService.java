package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

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
        filmStorage.addLike(filmId, userId);
        film.getLikes().add(userId);
        log.trace("Фильм '{}' теперь имеет {} лайков", film.getName(), film.getLikes().size());
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка у фильма id={} от пользователя id={}", filmId, userId);
        Film film = getFilmById(filmId);
        validateUserExists(userId);
        filmStorage.removeLike(filmId, userId);
        film.getLikes().remove(userId);
        log.trace("Фильм '{}' теперь имеет {} лайков", film.getName(), film.getLikes().size());
    }

    public List<Film> getMostPopular(int count) {
        return filmStorage.getPopularFilms(count);
    }

    public Film findById(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
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
