package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public List<Mpa> findAll() {
        String sql = "SELECT * FROM mpa ORDER BY mpa_id";
        return jdbcTemplate.query(sql, this::mapRowToMpa);
    }

    public Mpa findById(Long id) {
        String sql = "SELECT * FROM mpa WHERE mpa_id = ?";
        List<Mpa> result = jdbcTemplate.query(sql, this::mapRowToMpa, id);
        if (result.isEmpty()) {
            throw new NotFoundException("MPA с id=" + id + " не найден");
        }
        return result.get(0);
    }

    private Mpa mapRowToMpa(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Mpa mpa = new Mpa();
        mpa.setId(rs.getLong("mpa_id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}