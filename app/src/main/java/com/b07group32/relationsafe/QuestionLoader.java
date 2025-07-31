package com.b07group32.relationsafe;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class QuestionLoader {
    static Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(
                    RuntimeTypeAdapterFactory
                            .of(Choice.class, "type")
                            .registerSubtype(MultipleChoice.class, "multiple_choice")
                            .registerSubtype(MultipleResponseChoice.class, "multiple_response_choice")
                            .registerSubtype(FreeFormChoice.class, "freeform")
                            .registerSubtype(DropdownChoice.class, "dropdown")
                            .registerSubtype(DateChoice.class, "date"))
            .create();

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
