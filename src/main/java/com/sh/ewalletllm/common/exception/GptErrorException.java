package com.sh.ewalletllm.common.exception;

import lombok.Getter;

@Getter
public class GptErrorException extends RuntimeException{

    public GptErrorException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return "[OPEN AI Exception Occurred] : " + getMessage();
    }
}