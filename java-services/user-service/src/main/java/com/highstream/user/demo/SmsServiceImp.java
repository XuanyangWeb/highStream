package com.highstream.user.demo;

public class SmsServiceImp implements SmsService {

    @Override
    public String send(String message) {
        System.out.println("send Message : " + message);
        return message;
    }
}
