package com.example.tripservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class TripDraftExpiredException extends BaseException {
    public TripDraftExpiredException() {
        super("Draft has expired", ErrorType.GONE);
    }
}
