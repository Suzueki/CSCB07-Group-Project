package com.b07group32.relationsafe;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class QuestionnaireFragment extends Fragment {
    private List<Question> questions;
    int questionIndex = 0;
    private LinearLayout questionLayout;
    private TextView questionContent;
    private TextView questionNumber;
    private RadioGroup multipleChoice;
    private RadioButton choice1;
    private RadioButton choice2;
    private RadioButton choice3;
    private RadioButton choice4;
    private EditText shortResponse;
    private Button buttonBack;
    private Button buttonNext;
    private Button buttonSubmit;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);

        // Get views from the layout
        questions = loadQuestions();
        questionLayout = view.findViewById(R.id.questionLayout);
        questionContent = view.findViewById(R.id.questionContent);
        questionNumber = view.findViewById(R.id.questionNum);
        multipleChoice = view.findViewById(R.id.multipleChoice);
        choice1 = view.findViewById(R.id.choice1);
        choice2 = view.findViewById(R.id.choice2);
        choice3 = view.findViewById(R.id.choice3);
        choice4 = view.findViewById(R.id.choice4);
        shortResponse = view.findViewById(R.id.shortResponse);
        buttonBack = view.findViewById(R.id.buttonBack);
        buttonNext = view.findViewById(R.id.buttonNext);
        buttonSubmit = view.findViewById(R.id.buttonSubmit);

        if (!questions.isEmpty()) {
            displayQuestions(questionIndex);
        }
        return view;
    }

    private List<Question> loadQuestions() {
        // TODO: load questions from JSON
        // temporary implementation for testing
        ArrayList<Question> test = new ArrayList<>();
        test.add(new Question("What is your favorite color?", new ArrayList<Answer>(){{
            add(new MultipleChoiceAnswer("Red"));
            add(new MultipleChoiceAnswer("Green"));
            add(new MultipleChoiceAnswer("Yellow"));
            add(new MultipleChoiceAnswer("Blue"));
    }}));
        return test;
    }

    private void displayQuestions(int index) {
        Question question = questions.get(index);
        questionContent.setText(question.getQuestionText());
        questionNumber.setText("Question " + (index + 1) + "/" + questions.size());

        // Reset choices
        multipleChoice.clearCheck();
        shortResponse.setText("");

        List<Answer> answers = question.getAnswers();
        RadioButton[] choices = {choice1, choice2, choice3, choice4};

        for (int i = 0; i < choices.length; i++){
            if (i < answers.size()) {
                choices[i].setText(answers.get(i).getLabel());
                choices[i].setVisibility(View.VISIBLE);
            } else {
                choices[i].setVisibility(View.GONE);

            }
        }
    }
}
