package com.b07group32.relationsafe;

import android.content.Context;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class QuestionLoader {
    static Gson gson = new Gson();

    public static Form loadQuestions(Context context, String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            InputStreamReader isr = new InputStreamReader(is);
            return gson.fromJson(isr, Form.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
