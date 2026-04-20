package com.example.AUHT_SERVICE.UTILS;

import java.util.regex.Pattern;

public class PasswordSeguroUtil {

    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=." +
                    "*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    private static  final Pattern PASSWORD_PATTERN_PATTERN = Pattern.compile(PASSWORD_PATTERN);

    public static boolean validatePassword(String password) {
        return PASSWORD_PATTERN_PATTERN.matcher(password).matches();
    }
    public static String  GetError(){
        return "Contraseña debe tener: mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial (@$!%*?&)";
    }

}
