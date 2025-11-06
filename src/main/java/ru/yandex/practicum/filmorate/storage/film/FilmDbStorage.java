package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Qualifier("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MpaDbStorage mpaDbStorage;
    private final GenreDbStorage genreDbStorage;

    @Override
    public Collection<Film> findAll() {
        String sql = """
            SELECT f.*, m.name AS mpa_name, m.mpa_id
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.mpa_id
            ORDER BY f.film_id
        """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        loadGenresForFilms(films);
        loadLikesForFilms(films);
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = """
            SELECT f.*, m.name AS mpa_name, m.mpa_id
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.mpa_id
            WHERE f.film_id = ?
        """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.get(0);
        film.setGenres(findGenresByFilmId(film.getId()));
        film.setLikes(findLikesByFilmId(film.getId()));
        return Optional.of(film);
    }

    @Override
    public Film create(Film film) {
        validateFilm(film);
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            try {
                mpaDbStorage.findById(film.getMpa().getId());
            } catch (NotFoundException e) {
                throw new NotFoundException("MPA с id=" + film.getMpa().getId() + " не найден");
            }
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                try {
                    genreDbStorage.findById(genre.getId());
                } catch (NotFoundException e) {
                    throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                }
            }
        }

        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"film_id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            stmt.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return stmt;
        }, keyHolder);

        long newId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        film.setId(newId);

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            addGenresToFilm(film);
        }

        return findById(newId).orElseThrow(() ->
                new NotFoundException("Фильм с id=" + newId + " не найден после создания"));
    }

    @Override
    public Film update(Film film) {
        validateFilm(film);
        if (!existsById(film.getId())) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        if (film.getMpa() != null && film.getMpa().getId() != null) {
            try {
                mpaDbStorage.findById(film.getMpa().getId());
            } catch (NotFoundException e) {
                throw new NotFoundException("MPA с id=" + film.getMpa().getId() + " не найден");
            }
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                try {
                    genreDbStorage.findById(genre.getId());
                } catch (NotFoundException e) {
                    throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                }
            }
        }

        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        int rows = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (rows == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            addGenresToFilm(film);
        }

        return findById(film.getId()).orElseThrow(() ->
                new NotFoundException("Фильм с id=" + film.getId() + " не найден после обновления"));
    }

    @Override
    public void deleteById(Long id) {
        if (!existsById(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
    }

    @Override
    public void clear() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM films");
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        if (!existsById(filmId)) {
            throw new NotFoundException("Фильм с id=" + filmId + " не найден");
        }
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = """
        SELECT f.*, m.name AS mpa_name, m.mpa_id,
               COUNT(fl.user_id) AS likes_count
        FROM films f
        LEFT JOIN mpa m ON f.mpa_id = m.mpa_id
        LEFT JOIN film_likes fl ON f.film_id = fl.film_id
        GROUP BY f.film_id, m.mpa_id, m.name
        ORDER BY likes_count DESC, f.film_id ASC
        LIMIT ?
    """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        loadGenresForFilms(films);
        loadLikesForFilms(films);
        return films;
    }

    private Film mapRowToFilm(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        long id = rs.getLong("film_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        LocalDate releaseDate = rs.getDate("release_date").toLocalDate();
        int duration = rs.getInt("duration");
        Long mpaId = rs.getLong("mpa_id");
        String mpaName = rs.getString("mpa_name");

        Film film = new Film();
        film.setId(id);
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        if (mpaId != 0) {
            Mpa mpa = new Mpa();
            mpa.setId(mpaId);
            mpa.setName(mpaName);
            film.setMpa(mpa);
        }
        return film;
    }

    private void addGenresToFilm(Film film) {
        Set<Genre> uniqueGenres = film.getGenres().stream()
                .collect(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(Genre::getId))));

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        uniqueGenres.forEach(g -> jdbcTemplate.update(sql, film.getId(), g.getId()));

        film.setGenres(new ArrayList<>(uniqueGenres));
    }

    private List<Genre> findGenresByFilmId(Long filmId) {
        String sql = """
                SELECT g.genre_id, g.name
                FROM genres g
                INNER JOIN film_genres fg ON g.genre_id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.genre_id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getLong("genre_id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, filmId);
    }

    private Set<Long> findLikesByFilmId(Long filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));
        String sql = String.format("""
                SELECT fg.film_id, g.genre_id, g.name
                FROM film_genres fg
                INNER JOIN genres g ON fg.genre_id = g.genre_id
                WHERE fg.film_id IN (%s)
                ORDER BY fg.film_id, g.genre_id
                """, inSql);
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        Map<Long, List<Genre>> genresByFilm = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            long filmId = rs.getLong("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getLong("genre_id"));
            genre.setName(rs.getString("name"));
            genresByFilm.computeIfAbsent(filmId, ignored -> new ArrayList<>()).add(genre);
            return null;
        }, filmIds.toArray());
        films.forEach(film -> film.setGenres(genresByFilm.getOrDefault(film.getId(), Collections.emptyList())));
    }

    private void loadLikesForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));
        String sql = String.format("""
                SELECT fl.film_id, fl.user_id
                FROM film_likes fl
                WHERE fl.film_id IN (%s)
                """, inSql);
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        Map<Long, Set<Long>> likesByFilm = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            long filmId = rs.getLong("film_id");
            long userId = rs.getLong("user_id");
            likesByFilm.computeIfAbsent(filmId, ignored -> new HashSet<>()).add(userId);
            return null;
        }, filmIds.toArray());
        films.forEach(film -> {
            Set<Long> likes = likesByFilm.getOrDefault(film.getId(), Collections.emptySet());
            film.setLikes(likes);
        });
    }

    private boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации: название пустое");
            throw new ValidationException("Название не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Ошибка валидации: описание превышает 200 символов");
            throw new ValidationException("Максимальная длина описания составляет 200 символов");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.error("Ошибка валидации: дата релиза не корректна");
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() == null || film.getDuration() <= 0) {
            log.error("Ошибка валидации: продолжительность фильма <= 0");
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}