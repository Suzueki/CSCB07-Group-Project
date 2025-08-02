package com.b07group32.relationsafe;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class QuestionnaireFragment extends Fragment {
    // List of questions
    // Future idea: conform to open-closed principle
    private List<Question> questionRoute = null;
    private List<Question> warmUpQuestions;
    private List<Question> stillInRelationshipQuestions;
    private List<Question> planningToLeaveQuestions;
    private List<Question> postSeparationQuestions;
    private List<Question> followUpQuestions;
    private List<Question> questionList; // main question list to be loaded from
    private List<Question> nextQuestionList; // which set of questions to load next
    private List<Question> prevQuestionList; // keep track of which question set we came from
    private List<Question> startQuestionList;

    static int questionIndex = 0;
    private LinearLayout questionLayout;
    private TextView questionContent;
    private TextView questionNumber;
    private LinearLayout checkboxGroup;
    private CheckBox checkbox1;
    private CheckBox checkbox2;
    private CheckBox checkbox3;
    private CheckBox checkbox4;
    private RadioGroup choiceGroup;
    private RadioButton choice1;
    private RadioButton choice2;
    private RadioButton choice3;
    private RadioButton choice4;
    private EditText shortResponse;
    private AutoCompleteTextView dropdown;
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
        checkboxGroup = view.findViewById(R.id.selectMultiple);
        checkbox1 = view.findViewById(R.id.checkbox1);
        checkbox2 = view.findViewById(R.id.checkbox2);
        checkbox3 = view.findViewById(R.id.checkbox3);
        checkbox4 = view.findViewById(R.id.checkbox4);
        choiceGroup = view.findViewById(R.id.multipleChoice);
        choice1 = view.findViewById(R.id.choice1);
        choice2 = view.findViewById(R.id.choice2);
        choice3 = view.findViewById(R.id.choice3);
        choice4 = view.findViewById(R.id.choice4);
        shortResponse = view.findViewById(R.id.shortResponse);
        // Autocomplete Dropdown
        final String[] city = new String[1]; // acts like a mutable container
        ArrayList<String> cities = new ArrayList<>(); //store cities into here from JSON
        dropdown = view.findViewById(R.id.dropdown);

        buttonBack = view.findViewById(R.id.buttonBack);
        buttonNext = view.findViewById(R.id.buttonNext);
        buttonSubmit = view.findViewById(R.id.buttonSubmit);

        // TODO: set button's text differently for editing question response
        buttonBack.setText(R.string.string_back);
        buttonNext.setText(R.string.string_next);
        buttonSubmit.setText(R.string.string_submit);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayNextQuestion();

            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { displayPreviousQuestion(); }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cities);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                city[0] = adapter.getItem(position).toString();
            }
        });
        dropdown.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                city[0] = null;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadAllQuestions();
        startQuestionList = warmUpQuestions;

        // TODO: initial loaded list of questions can be changed, perhaps using parameter from fragment
        questionList = startQuestionList;
        nextQuestionList = null;
        prevQuestionList = null;

        if (questionList != null && !questionList.isEmpty()) {
            displayQuestion(questionList.get(questionIndex).getQuestionId(), questionList);
        } else {
            Toast.makeText(getContext(), "No questions found", Toast.LENGTH_SHORT).show();
        }

        // TK: temporary
        questionRoute = stillInRelationshipQuestions;

        return view;
    }

    private void setButtonVisibility() {
        if (questionIndex == 0 && prevQuestionList == null) {
            buttonBack.setVisibility(View.GONE);
        } else {
            buttonBack.setVisibility(View.VISIBLE);
        }
        if (questionIndex == questionList.size() - 1 && nextQuestionList == null) {
            buttonNext.setVisibility(View.GONE);
            buttonSubmit.setVisibility(View.VISIBLE);

        } else {
            buttonNext.setVisibility(View.VISIBLE);
            buttonSubmit.setVisibility(View.GONE);
        }
    }

    private void getResponse() {

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

        List<Choice> choices = question.getChoices();
        RadioButton[] buttons = {choice1, choice2, choice3, choice4};
        CheckBox[] checkBoxes = {checkbox1, checkbox2, checkbox3, checkbox4};

        // Reset choices
        for (CheckBox checkbox : checkBoxes) {
            checkbox.setChecked(false);
        }
        choiceGroup.clearCheck();
        shortResponse.setText("");


        // Set all to default invisible
        checkboxGroup.setVisibility(View.GONE);
        for (int i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i].setVisibility(View.GONE);
        }
        choiceGroup.setVisibility(View.GONE);
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setVisibility(View.GONE);
        }
        shortResponse.setVisibility(View.GONE);
        dropdown.setVisibility(View.GONE);

        // Set visibility and text for each choice
        for (int i = 0; i < choices.size(); i++) {
            Choice choice = choices.get(i);
            switch (choice.getType()) {
                case "multiple_choice":
                    choiceGroup.setVisibility(View.VISIBLE);
                    buttons[i].setText(choice.getChoice());
                    buttons[i].setVisibility(View.VISIBLE);
                    break;

                case "checkbox":
                    checkboxGroup.setVisibility(View.VISIBLE);
                    checkBoxes[i].setText(choice.getChoice());
                    checkBoxes[i].setVisibility(View.VISIBLE);
                    break;

                case "multiple_response_choice":
                    choiceGroup.setVisibility(View.VISIBLE);
                    buttons[i].setText(choice.getChoice());
                    buttons[i].setVisibility(View.VISIBLE);
                    shortResponse.setHint(((MultipleResponseChoice) choice).getHint());
                    shortResponse.setVisibility(View.VISIBLE);
                    break;
                case "dropdown":
                    dropdown.setVisibility(View.VISIBLE);
                    dropdown.setText(choice.getChoice());
                    break;
                case "freeform":
                    shortResponse.setHint(((FreeFormChoice) choice).getHint() == null ?
                            "Enter a response" : ((FreeFormChoice) choice).getHint());
                    shortResponse.setVisibility(View.VISIBLE);
                    break;
            }
        }
        setButtonVisibility();
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
        questionIndex++;
        setQuestionLists();
        if (questionIndex >= questionList.size()) {
            prevQuestionList = questionList;
            questionList = nextQuestionList;
            nextQuestionList = null;
            questionIndex = 0;
        }
        Toast.makeText(getContext(), Integer.toString(questionIndex), Toast.LENGTH_SHORT).show();
        setQuestionView(questionIndex, questionList);
    }

    private void setQuestionLists() {
        // TODO: make able to change order of questions
        if (questionIndex <= 0 || questionIndex >= questionList.size() - 1) { // first or last question
            if (questionList.equals(warmUpQuestions)) {
                nextQuestionList = questionRoute;
                prevQuestionList = null;
            } else if (questionList.equals(stillInRelationshipQuestions) ||
                    questionList.equals(planningToLeaveQuestions) ||
                    questionList.equals(postSeparationQuestions)) {
                nextQuestionList = followUpQuestions;
                prevQuestionList = warmUpQuestions;
            } else if (questionList.equals(followUpQuestions)) {
                nextQuestionList = null;
                prevQuestionList = questionRoute;
            }
        }
    }

    private void displayPreviousQuestion() {
        questionIndex--;
        setQuestionLists();
        if (questionIndex < 0 && prevQuestionList != null) {
            nextQuestionList = questionList;
            questionList = prevQuestionList;
            prevQuestionList = null;
            questionIndex = questionList.size() - 1;
        }
        setQuestionView(questionIndex, questionList);
    }

    private void setQuestionIndex(int index) {
        questionIndex = index;
    }


}
