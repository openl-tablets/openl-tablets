package org.openl.rules.spring.openapi.app100;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.spring.openapi.app100.model.Animal;

@RestController
public class AnimalController {

    @GetMapping(value = "/animals", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Animal> getAnimals() {
        return null;
    }

    @PostMapping(value = "/animals", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Animal createAnimal(@RequestBody Animal animal) {
        return null;
    }
}
