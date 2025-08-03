package com.b07group32.relationsafe;
import android.app.DatePickerDialog;
import android.icu.util.Calendar;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionnaireFragment extends Fragment {

    // Constants for fragment arguments
    private static final String ARG_MODE = "mode";
    private static final String ARG_QUESTION_ID = "question_id";

    // Instance variables for fragment arguments
    private String mode;
    private String questionId;

    private PlanDatabaseManager db;

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
    private TextView date;
    private Button selectDateButton;
    private Button buttonBack;
    private Button buttonNext;
    private Button buttonSubmit;
    private ArrayList<TipContainer> tips;

    /**
     * Factory method to create a new instance of QuestionnaireFragment
     * @param mode The mode of operation ("edit", "change branch", etc.)
     * @param questionId The ID of the question to display/edit
     * @return A new instance of QuestionnaireFragment
     */
    public static QuestionnaireFragment newInstance(String mode, String questionId) {
        QuestionnaireFragment fragment = new QuestionnaireFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        args.putString(ARG_QUESTION_ID, questionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getString(ARG_MODE);
            questionId = getArguments().getString(ARG_QUESTION_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);
        // Initialize Firebase database manager
        db = new PlanDatabaseManager();

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

        date = view.findViewById(R.id.dateText);
        selectDateButton = view.findViewById(R.id.selectDateButton);

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
                getResponse();
                displayNextQuestion();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { displayPreviousQuestion(); }
        });

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getResponse();
                Toast.makeText(getContext(), "Response submitted", Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }
        });

        selectDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
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

        // Handle different modes
        if (mode != null && questionId != null) {
            handleModeAndQuestionId();
        } else {
            // Default behavior when no arguments are provided
            startQuestionList = warmUpQuestions;
            questionList = startQuestionList;
            nextQuestionList = null;
            prevQuestionList = null;

            if (questionList != null && !questionList.isEmpty()) {
                displayQuestion(questionList.get(questionIndex).getQuestionId(), questionList);
            } else {
                Toast.makeText(getContext(), "No questions found", Toast.LENGTH_SHORT).show();
            }
        }

        // initial route
        questionRoute = planningToLeaveQuestions;

        return view;
    }

    private void handleModeAndQuestionId() {
        switch (mode) {
            case "edit":
                // Find the question in all question lists and display it for editing
                List<Question> targetList = findQuestionList(questionId);
                if (targetList != null) {
                    questionList = targetList;
                    displayQuestion(questionId, questionList);
                    // Adjust button text for edit mode
                    buttonNext.setText("Save");
                    buttonBack.setText("Cancel");
                } else {
                    Toast.makeText(getContext(), "Question not found", Toast.LENGTH_SHORT).show();
                }
                break;

            case "change branch":
                // Handle branch change logic - this might involve going back to warm-up questions
                // or changing the question route
                if (questionId.equals("w1")) {
                    questionList = warmUpQuestions;
                    displayQuestion(questionId, questionList);
                    buttonNext.setText("Update Branch");
                }
                break;

            default:
                // Default behavior
                startQuestionList = warmUpQuestions;
                questionList = startQuestionList;
                displayQuestion(questionList.get(0).getQuestionId(), questionList);
                break;
        }
    }

    private List<Question> findQuestionList(String qId) {
        // Search through all question lists to find which one contains the question ID
        if (searchQuestionInList(warmUpQuestions, qId)) return warmUpQuestions;
        if (searchQuestionInList(stillInRelationshipQuestions, qId)) return stillInRelationshipQuestions;
        if (searchQuestionInList(planningToLeaveQuestions, qId)) return planningToLeaveQuestions;
        if (searchQuestionInList(postSeparationQuestions, qId)) return postSeparationQuestions;
        if (searchQuestionInList(followUpQuestions, qId)) return followUpQuestions;
        return null;
    }

    private boolean searchQuestionInList(List<Question> questions, String qId) {
        if (questions == null) return false;
        for (Question q : questions) {
            if (q.getQuestionId().equals(qId)) {
                return true;
            }
        }
        return false;
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
        // Consume current state of all choices

        // Callback method to change route based on response
        PlanDatabaseManager.PlanUpdateCallback callback = new PlanDatabaseManager.PlanUpdateCallback() {
            @Override
            public void onSuccess(String action, String qid, String response) {
                if (qid.equals("w1")) {
                    switch (response) { // Bad: Hardcoded to values in JSON
                        case "Still in a relationship":
                            questionRoute = stillInRelationshipQuestions;
                            break;
                        case "Planning to leave":
                            questionRoute = planningToLeaveQuestions;
                            break;
                        case "Post-separation":
                            questionRoute = postSeparationQuestions;
                            break;
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        };
        HashMap<String, Object> response = new HashMap<>();
        String qid = questionList.get(questionIndex).getQuestionId();
        String answer = "";
        String tip = "";

        RadioButton[] buttons = {choice1, choice2, choice3, choice4};
        CheckBox[] checkBoxes = {checkbox1, checkbox2, checkbox3, checkbox4};

        // Handle each response type (bad for expandability)
        // Future: refactor to conform to design principles
        if (choiceGroup.getVisibility() == View.VISIBLE) {
            for (RadioButton button : buttons) {
                if (button.isChecked()) {
                    tip = TipSearcher.findMatchingTip(tips,
                            questionList.get(questionIndex).getQuestionId(),
                            button.getText().toString());
                    answer = button.getText().toString();
                    break;
                }
            }
        }
        if (checkboxGroup.getVisibility() == View.VISIBLE) {
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isChecked()) {
                    answer = answer.concat(checkBox.getText().toString()).concat(", ");
                    tip = TipSearcher.findMatchingTip(tips, questionList.get(questionIndex).getQuestionId(), null);
                }
            }
            answer = strip(answer);
        }
        if (shortResponse.getVisibility() == View.VISIBLE) {
            //answer = answer.concat(shortResponse.getText().toString());
            answer = answer + " " + shortResponse.getText().toString();
            tip = TipSearcher.findMatchingTip(tips, questionList.get(questionIndex).getQuestionId(), answer);
        }
        if (dropdown.getVisibility() == View.VISIBLE) {
            answer = dropdown.getText().toString();
            tip = TipSearcher.findMatchingTip(tips, questionList.get(questionIndex).getQuestionId(), answer);
        }
        if (date.getVisibility() == View.VISIBLE) {
            answer = date.getText().toString();
            tip = TipSearcher.findMatchingTip(tips, questionList.get(questionIndex).getQuestionId(), answer);
        }

        // Add to db
        if (answer.isBlank()) {
            return;
        }
        if (tip != null && !tip.isBlank()) {
            String[] answerParts = answer.split(" ");
            tip = tip.replace("{answer}", answerParts.length == 2 ? answerParts[1] : answer);
        }
        response.put("qid", qid);
        response.put("answer", answer);
        response.put("tip", tip);
        db.addResponse(response, callback);
    }

    @NonNull
    private static String strip(String answer) {
        if (answer.endsWith(", ")) {
            answer = answer.substring(0, answer.length() - 2);
        }
        return answer;
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    date.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void getDate(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
        String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
        date.setText(selectedDate);
    }

    private List<Question> sortQuestionsById(List<Question> questions) {
        // Sort questions by ID
        questions.sort((q1, q2) -> q1.getQuestionId().compareTo(q2.getQuestionId()));
        return questions;
    }

    private void loadAllQuestions() {
        Form form = QuestionLoader.loadQuestions(getContext(), "questions_and_tips.json");
        if (form == null || form.warm_up == null) {
            return;
        }
        warmUpQuestions = sortQuestionsById(form.warm_up);
        stillInRelationshipQuestions = sortQuestionsById(form.still_in_relationship);
        planningToLeaveQuestions = sortQuestionsById(form.planning_to_leave);
        postSeparationQuestions = sortQuestionsById(form.post_separation);
        followUpQuestions = sortQuestionsById(form.follow_up);
        tips = form.tips;
    }

    private void setQuestionView(int index, List<Question> questions) {
        Question question = questions.get(index);
        questionContent.setText(question.getQuestionText());
        questionNumber.setText("Question " + (index + 1) + "/" + questions.size());

        List<Choice> choices = question.getChoices();
        RadioButton[] buttons = {choice1, choice2, choice3, choice4};
        CheckBox[] checkBoxes = {checkbox1, checkbox2, checkbox3, checkbox4};

        resetUIElements(checkBoxes, buttons, questions.get(index).getQuestionId());

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
                    shortResponse.setEnabled(true);
                    shortResponse.setVisibility(View.VISIBLE);
                    break;
                case "date":
                    date.setText(choice.getChoice());
                    date.setVisibility(View.VISIBLE);
                    selectDateButton.setVisibility(View.VISIBLE);
                    break;
            }
        }
        setButtonVisibility();
    }

    private void resetUIElements(CheckBox[] checkBoxes, RadioButton[] buttons, String qid) {
        // Reset choices
        for (CheckBox checkbox : checkBoxes) {
            checkbox.setChecked(false);
        }
        choiceGroup.clearCheck();
        shortResponse.setText("");

        date.setText("");
        date.setVisibility(View.GONE);
        selectDateButton.setVisibility(View.GONE);

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

    }

    private void repopulateUI(String qid) {
        // Set enabled based on previous results
        DatabaseReference ref = db.getDatabase();
        Query query = ref.orderByChild("qid").equalTo(qid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Map<String, Object> response = (Map<String, Object>) data.getValue();


                        String selectedAnswer = (String) response.get("answer");
                        if (selectedAnswer != null) {
                            switch (selectedAnswer) {
                                case "option1":
                                    choice1.setChecked(true);
                                    break;
                                case "option2":
                                    choice2.setChecked(true);
                                    break;
                                case "option3":
                                    choice3.setChecked(true);
                                    break;
                                case "option4":
                                    choice4.setChecked(true);
                                    break;
                            }
                        }

                        // Example for multiple-choice (checkboxes)
                        List<String> selectedAnswers = (List<String>) response.get("answers");
                        if (selectedAnswers != null) {
                            checkbox1.setChecked(selectedAnswers.contains("option1"));
                            checkbox2.setChecked(selectedAnswers.contains("option2"));
                            // etc.
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // Display a question based on its ID
    // Intended for editing question response
    private void displayQuestion(String questionId, List<Question> questions) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getQuestionId().equals(questionId)) {
                questionIndex = i; // Update the index to match the found question
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