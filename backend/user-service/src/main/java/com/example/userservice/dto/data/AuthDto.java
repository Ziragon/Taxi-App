package com.example.userservice.dto.data;

public record AuthDto(

        AccountDto accountDto,

        TokenDto accessTokenDto,

        TokenDto refreshTokenDto,

        PassengerProfileDto passengerProfileDto,

        DriverProfileDto driverProfileDto
) {}
