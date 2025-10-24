package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public void addLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId);
        validateUserExists(userId);
        film.getLikes().add(userId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = getFilmById(filmId);
        validateUserExists(userId);
        film.getLikes().remove(userId);
    }

    public List<Film> getMostPopular(int count) {
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateUserExists(Long userId) {
        boolean exists = userStorage.findAll().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (!exists) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private Film getFilmById(Long filmId) {
        return filmStorage.findAll().stream()
                .filter(f -> f.getId(). equals(filmId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
    }
}
