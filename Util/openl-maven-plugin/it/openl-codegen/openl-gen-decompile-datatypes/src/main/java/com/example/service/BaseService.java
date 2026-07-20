package com.example.service;

public interface BaseService {

    String hello(int hour);

    default String ping() {
        return "Pong!";
    }
}
