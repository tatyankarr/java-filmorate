package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, MpaDbStorage.class, GenreDbStorage.class})
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Test
    @DisplayName("Создание и получение фильма по ID")
    void testCreateAndFindFilmById() {
        Film f1 = new Film();
        f1.setName("Film 1");
        f1.setDescription("Description 1");
        f1.setReleaseDate(LocalDate.of(2023, 1, 1));
        f1.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1L);
        f1.setMpa(mpa);

        Film created = filmStorage.create(f1);

        Optional<Film> found = filmStorage.findById(created.getId());

        assertThat(found)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getName()).isEqualTo("Film 1");
                    assertThat(f.getDescription()).isEqualTo("Description 1");
                    assertThat(f.getDuration()).isEqualTo(120);
                    assertThat(f.getMpa().getId()).isEqualTo(1L);
                });
    }

    @Test
    @DisplayName("Обновление фильма")
    void testUpdateFilm() {
        Film f1 = new Film();
        f1.setName("Original Film");
        f1.setDescription("Original Description");
        f1.setReleaseDate(LocalDate.of(2022, 5, 5));
        f1.setDuration(100);

        Mpa mpa = new Mpa();
        mpa.setId(1L);
        f1.setMpa(mpa);

        Film created = filmStorage.create(f1);

        created.setName("Updated Film");
        created.setDescription("Updated Description");

        filmStorage.update(created);

        Optional<Film> updated = filmStorage.findById(created.getId());

        assertThat(updated)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getName()).isEqualTo("Updated Film");
                    assertThat(f.getDescription()).isEqualTo("Updated Description");
                });
    }

    @Test
    @DisplayName("Удаление фильма")
    void testDeleteFilm() {
        Film f1 = new Film();
        f1.setName("To Delete");
        f1.setDescription("Description");
        f1.setReleaseDate(LocalDate.of(2021, 1, 1));
        f1.setDuration(90);

        Mpa mpa = new Mpa();
        mpa.setId(1L);
        f1.setMpa(mpa);

        Film created = filmStorage.create(f1);
        filmStorage.deleteById(created.getId());

        Optional<Film> found = filmStorage.findById(created.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Получение всех фильмов")
    void testFindAllFilms() {
        filmStorage.clear();

        Film f1 = new Film();
        f1.setName("Film A");
        f1.setDescription("Desc A");
        f1.setReleaseDate(LocalDate.of(2020, 2, 2));
        f1.setDuration(110);
        Mpa mpa1 = new Mpa();
        mpa1.setId(1L);
        f1.setMpa(mpa1);

        Film f2 = new Film();
        f2.setName("Film B");
        f2.setDescription("Desc B");
        f2.setReleaseDate(LocalDate.of(2021, 3, 3));
        f2.setDuration(130);
        Mpa mpa2 = new Mpa();
        mpa2.setId(1L);
        f2.setMpa(mpa2);

        filmStorage.create(f1);
        filmStorage.create(f2);

        Collection<Film> films = filmStorage.findAll();

        assertThat(films)
                .hasSize(2)
                .extracting(Film::getName)
                .containsExactlyInAnyOrder("Film A", "Film B");
    }

    @Test
    @DisplayName("Удаление несуществующего фильма")
    void testDeleteNonExistentFilm() {
        assertThrows(RuntimeException.class, () -> filmStorage.deleteById(999L));
    }
}
