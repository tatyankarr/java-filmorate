package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {

    private final MpaDbStorage mpaStorage;

    @GetMapping
    public List<Mpa> findAll() {
        return mpaStorage.findAll();
    }

    @GetMapping("/{id}")
    public Mpa findById(@PathVariable Long id) {
        return mpaStorage.findById(id);
    }
}