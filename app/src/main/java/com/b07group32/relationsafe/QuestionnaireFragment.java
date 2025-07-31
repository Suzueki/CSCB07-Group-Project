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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class QuestionnaireFragment extends Fragment {
    // List of questions
    // Future idea: conform to open-closed principle
    private List<Question> warmUpQuestions;
    private List<Question> stillInRelationshipQuestions;
    private List<Question> planningToLeaveQuestions;
    private List<Question> postSeparationQuestions;
    private List<Question> followUpQuestions;
    private List<Question> questionList; // main question list to be loaded from

    static int questionIndex = 0;
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
        // TODO: set button's text differently for editing question response
        buttonBack.setText(R.string.string_back);
        buttonNext.setText(R.string.string_next);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayNextQuestion();
            }
        });

        loadAllQuestions();

        // TODO: initial loaded list of questions can be changed, perhaps using parameter from fragment
        questionList = warmUpQuestions;

        if (questionList != null && !questionList.isEmpty()) {
            displayNextQuestion();
        } else {
            Toast.makeText(getContext(), "No questions found", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    // think this is useless
    private void logQuestions(List<Question> questions) {
        for (Question question : questions) {
            System.out.println("Question: " + question.getQuestionText());
            List<Choice> choices = question.getChoices();
            for (Choice choice : choices) {
                System.out.println("Answer: " + choice.getResponse());
            }
        }
    }

    private List<Question> sortQuestionsById(List<Question> questions) {
        // Sort questions by ID
        questions.sort((q1, q2) -> q1.getQuestionId().compareTo(q2.getQuestionId()));
        return questions;
    }
    private void loadAllQuestions() {
        Form form = QuestionLoader.loadQuestions(getContext(), "test_questions_and_tips.json");
        if (form == null || form.warm_up == null) {
            return;
        }
        warmUpQuestions = sortQuestionsById(form.warm_up);
        stillInRelationshipQuestions = sortQuestionsById(form.still_in_relationship);
        planningToLeaveQuestions = sortQuestionsById(form.planning_to_leave);
        postSeparationQuestions = sortQuestionsById(form.post_separation);
        followUpQuestions = sortQuestionsById(form.follow_up);
    }

    private void setQuestionView(int index, List<Question> questions) {
        Question question = questions.get(index);
        questionContent.setText(question.getQuestionText());
        questionNumber.setText("Question " + (index + 1) + "/" + questions.size());

        // Reset choices
        multipleChoice.clearCheck();

        List<Choice> choices = question.getChoices();
        RadioButton[] buttons = {choice1, choice2, choice3, choice4};

        // Set visibility and text for each choice
        for (int i = 0; i < buttons.length; i++){
            if (i < choices.size()) {
                Choice choice = choices.get(i);
                buttons[i].setText(choice.getResponse());
                buttons[i].setVisibility(View.VISIBLE);

                if (choice instanceof MultipleResponseChoice || choice instanceof FreeFormChoice) {
                    shortResponse.setVisibility(View.VISIBLE);
                    shortResponse.setText(((Respondable) choice).getHint() == null ?
                            "" : ((Respondable) choice).getHint());
                } else {
                    shortResponse.setVisibility(View.GONE);
                }

            } else {
                buttons[i].setVisibility(View.GONE);

            }
        }
    }

    // Display a question based on its ID
    // Intended for editing question response
    private void displayQuestion(String questionId, List<Question> questions) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getQuestionId().equals(questionId)) {
                setQuestionView(i, questions);
                return;
            }
        }
    }

    // Expect an ordered list of questions; every time called displays the next question in list
    // using static index
    // Intended for full questionnaire
    private void displayNextQuestion() {
        if (questionIndex >= questionList.size()) {
            Toast.makeText(getContext(), "End of questions", Toast.LENGTH_SHORT).show();
            return;
        }
        setQuestionView(questionIndex, questionList);
        questionIndex++;
    }

    private void setQuestionIndex(int index) {
        questionIndex = index;
    }


}
